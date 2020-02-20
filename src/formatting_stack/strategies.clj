(ns formatting-stack.strategies
  "Strategies are concerned with generating a seq of filenames to process (format, lint, or compile).

  They are configured to run in a determinate order.

  In practice, a strategy is function that receives a seq of filenames, and returns another:

  * more filenames may be added; and or
  * the passed filenames may be `filter`ed.

  A strategy may not return nil."
  (:require
   [clojure.string :as str]
   [clojure.tools.namespace.repl :refer [refresh-dirs]]
   [formatting-stack.protocols.spec :as protocols.spec]
   [formatting-stack.strategies.impl :as impl]
   [formatting-stack.util :refer [read-ns-decl require-lock try-require]]
   [nedap.speced.def :as speced]
   [nedap.utils.spec.api :refer [check!]])
  (:import
   (java.io File)))

(speced/defn all-files
  "This strategy unconditionally processes all files."
  [& {:keys [^::protocols.spec/filenames files]}]
  (let [tracked (->> (impl/file-entries "git" "ls-files")
                     (impl/absolutize "git"))
        untracked (->> (impl/file-entries "git" "ls-files" "--others" "--exclude-standard")
                       (impl/absolutize "git"))]
    (->> files
         (into tracked)
         (into untracked)
         (impl/extract-clj-files))))

(speced/defn git-completely-staged
  "This strategy processes the new or modified files that are _completely_ staged with git."
  [& {:keys [^::protocols.spec/filenames files, impl]
      :or   {impl (impl/file-entries "git" "status" "--porcelain")}}]
  (->> impl
       (filter #(re-find impl/git-completely-staged-regex %))
       (map #(str/replace-first % impl/git-completely-staged-regex ""))
       (map (fn [s]
              ;; for renames:
              (-> s (str/split #" -> ") last)))
       (impl/absolutize "git")
       (impl/extract-clj-files)
       (into files)))

(speced/defn git-not-completely-staged
  "This strategy processes all files that are not _completely_ staged with git. Untracked files are also included."
  [& {:keys [^::protocols.spec/filenames files, impl]
      :or   {impl (impl/file-entries "git" "status" "--porcelain")}}]
  (->> impl
       (filter #(re-find impl/git-not-completely-staged-regex %))
       (map #(str/replace-first % impl/git-not-completely-staged-regex ""))
       (impl/absolutize "git")
       (impl/extract-clj-files)
       (into files)))

(defn git-diff-against-default-branch
  "This strategy processes all files that this branch has modified.
  The diff is compared against the `:target-branch` option."
  [& {:keys [target-branch impl files blacklist]
      :or   {target-branch "master"
             impl          (impl/file-entries "git" "diff" "--name-only" target-branch)
             blacklist     (git-not-completely-staged :files [])}}]
  (->> impl
       (impl/absolutize "git")
       (remove (set blacklist))
       (impl/extract-clj-files)
       (into files)))

(speced/defn exclude-clj
  "This strategy excludes .clj files; .cljc files are not excluded in any case."
  [& {:keys [^::protocols.spec/filenames files]}]
  (->> files
       (remove (partial re-find #"\.clj$"))))

(speced/defn exclude-cljc
  "This strategy excludes .cljc files; .cljs files are not excluded in any case."
  [& {:keys [^::protocols.spec/filenames files]}]
  (->> files
       (remove (partial re-find #"\.cljc$"))))

(speced/defn exclude-cljs
  "This strategy excludes .cljs files; .cljc files are not excluded in any case."
  [& {:keys [^::protocols.spec/filenames files]}]
  (->> files
       (remove (partial re-find #"\.cljs$"))))

(speced/defn exclude-edn
  "This strategy excludes .edn files."
  [& {:keys [^::protocols.spec/filenames files]}]
  (->> files
       (remove (partial re-find #"\.edn$"))))

(speced/defn exclusively-cljs
  "This strategy excludes files not suffixed in .cljs or .cljc"
  [& {:keys [^::protocols.spec/filenames files]}]
  (->> files
       (filter (partial re-find #"\.clj[cs]$"))))

(speced/defn files-with-a-namespace
  "This strategy excludes files that don't begin with a `(ns ...)` form."
  [& {:keys [^::protocols.spec/filenames files]}]
  (->> files
       (filter read-ns-decl)))

(speced/defn jvm-requirable-files
  "This strategy excludes files that can't be `require`d under JVM Clojure."
  [& {:keys [^::protocols.spec/filenames files]}]
  (->> files
       (filter try-require)))

(speced/defn do-not-use-cached-results!
  "Normally, subsequent 'members' (formatters, linters, processors)
  using identical strategies will cache the results of those strategies.
  That is apt for formatters that do safe modifications, but not for more dangerous formatters.

  By adding this empty strategy, it is signaled that the member using it should not use a cached result.

  You can find a detailed explanation/example in https://git.io/fh7E0 ."
  [& {:keys [^::protocols.spec/filenames files]}]
  files)

(speced/defn namespaces-within-refresh-dirs-only
  "This strategy excludes the files that are Clojure/Script namespaces
  but are placed outside `#'clojure.tools.namespace.repl/refresh-dirs`.

  This variable must be set beforehand, and all its values should correspond to existing folders (relative to the project root).

  Files such as project.clj, or .edn files, etc are not excluded, since they aren't namespaces.

  The rationale for this strategy is allowing you to create clj namespace directories that are excluded from `refresh-dirs`.
  e.g. protocol definitions,
  and then ensuring that code-evaluating tools such as refactor-nrepl or Eastwood also respect that exclusion.

  That can avoid some code-reloading issues related to duplicate `defprotocol` definitions, etc."
  [& {:keys [^::protocols.spec/filenames files]}]
  {:pre [(check! seq                            refresh-dirs
                 (partial every? (speced/fn [^string? refresh-dir]
                                   (let [file (-> refresh-dir File.)]
                                     (if (-> file .exists)
                                       (-> file .isDirectory)
                                       ;; allow non-existing directories.
                                       ;; Temporary, until https://git.io/Jeaah is a thing:
                                       true)))) refresh-dirs)]}
  (->> files
       (filter (speced/fn [^string? filename]
                 (if-not (read-ns-decl filename)
                   true
                   (let [file (-> filename File.)]
                     (->> refresh-dirs
                          (some (fn [dir]
                                  (impl/dir-contains? dir file))))))))))

(defn refactor-nrepl-available? []
  (locking require-lock
    (try
      (require 'refactor-nrepl.ns.clean-ns)
      true
      (catch Throwable _
        false))))

(speced/defn when-refactor-nrepl
  "This strategy leaves all `files` as-is iff the `refactor-nrepl` library is in the classpath;
  else all `files` will be filtered out."
  [& {:keys [^::protocols.spec/filenames files]}]
  (if (refactor-nrepl-available?)
    files
    []))
