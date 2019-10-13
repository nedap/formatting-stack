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
   [formatting-stack.formatters.clean-ns.impl]
   [formatting-stack.strategies.impl :as impl]
   [formatting-stack.util :refer [try-require]]
   [nedap.speced.def :as speced]
   [nedap.utils.spec.api :refer [check!]])
  (:import
   (java.io File)))

(defn all-files
  "This strategy unconditionally processes all files."
  [& {:keys [files]}]
  (let [tracked (impl/file-entries "git" "ls-files")
        untracked (impl/file-entries "git" "ls-files" "--others" "--exclude-standard")]
    (->> files
         (into tracked)
         (into untracked)
         (impl/extract-clj-files))))

(defn git-completely-staged
  "This strategy processes the new or modified files that are _completely_ staged with git."
  [& {:keys [files impl]
      :or   {impl (impl/file-entries "git" "status" "--porcelain")}}]
  (->> impl
       (filter #(re-find impl/git-completely-staged-regex %))
       (map #(str/replace-first % impl/git-completely-staged-regex ""))
       (map (fn [s]
              ;; for renames:
              (-> s (str/split #" -> ") last)))
       (impl/extract-clj-files)
       (into files)))

(defn git-not-completely-staged
  "This strategy processes all files that are not _completely_ staged with git. Untracked files are also included."
  [& {:keys [files impl]
      :or   {impl (impl/file-entries "git" "status" "--porcelain")}}]
  (->> impl
       (filter #(re-find impl/git-not-completely-staged-regex %))
       (map #(str/replace-first % impl/git-not-completely-staged-regex ""))
       (impl/extract-clj-files)
       (into files)))

(defn git-diff-against-default-branch
  "This strategy processes all files that this branch has modified.
  The diff is compared against the `:target-branch` option."
  [& {:keys [target-branch impl files blacklist]
      :or   {target-branch "master"
             impl          (impl/file-entries "git" "diff" "--name-only" target-branch)
             blacklist     (git-not-completely-staged)}}]
  (->> impl
       (remove (set blacklist))
       impl/extract-clj-files
       (into files)))

(defn exclude-clj
  "This strategy excludes .clj files; .cljc files are not excluded in any case."
  [& {:keys [files]}]
  (->> files
       (remove (partial re-find #"\.clj$"))))

(defn exclude-cljc
  "This strategy excludes .cljc files; .cljs files are not excluded in any case."
  [& {:keys [files]}]
  (->> files
       (remove (partial re-find #"\.cljc$"))))

(defn exclude-cljs
  "This strategy excludes .cljs files; .cljc files are not excluded in any case."
  [& {:keys [files]}]
  (->> files
       (remove (partial re-find #"\.cljs$"))))

(defn exclude-edn
  "This strategy excludes .edn files."
  [& {:keys [files]}]
  (->> files
       (remove (partial re-find #"\.edn$"))))

(defn exclusively-cljs
  "This strategy excludes files not suffixed in .cljs or .cljc"
  [& {:keys [files]}]
  (->> files
       (filter (partial re-find #"\.clj[cs]$"))))

(defn files-with-a-namespace
  "This strategy excludes files that don't begin with a `(ns ...)` form."
  [& {:keys [files]}]
  (->> files
       (filter formatting-stack.formatters.clean-ns.impl/ns-form-of)))

(defn jvm-requirable-files
  "This strategy excludes files that can't be `require`d under JVM Clojure."
  [& {:keys [files]}]
  (->> files
       (filter try-require)))

(defn do-not-use-cached-results!
  "Normally, subsequent 'members' (formatters, linters, compilers)
  using identical strategies will cache the results of those strategies.
  That is apt for formatters that do safe modifications, but not for more dangerous formatters.

  By adding this empty strategy, it is signaled that the member using it should not use a cached result.

  You can find a detailed explanation/example in https://git.io/fh7E0 ."
  [& {:keys [files]}]
  files)

(defn namespaces-within-refresh-dirs-only
  "This stragegy excludes the files that are Clojure/Script namespaces
  but are placed outside `#'clojure.tools.namespace.repl/refresh-dirs`.

  This variable must be set beforehand, and all its values must correspond to existing folders (relative to the project root).

  Files such as project.clj, or .edn files, etc are not excluded, since they aren't namespaces.

  The rationale for this strategy is allowing you to create clj namespace directories that are excluded from `refresh-dirs`.
  e.g. protocol definitions,
  and then ensuring that code-evaluating tools such as refactor-nrepl or Eastwood also respect that exclusion.

  That can avoid some code-reloading issues related to duplicate `defprotocol` definitions, etc."
  [& {:keys [files]}]
  {:pre [(check! seq                                                 refresh-dirs
                 (partial every? (speced/fn [^string? refresh-dir]
                                   (let [file (-> refresh-dir File.)]
                                     (and (-> file .exists)
                                          (-> file .isDirectory))))) refresh-dirs)]}
  (->> files
       (filter (speced/fn [^string? filename]
                 (if-not (formatting-stack.formatters.clean-ns.impl/ns-form-of filename)
                   true
                   (let [file (-> filename File.)]
                     (->> refresh-dirs
                          (some (fn [dir]
                                  (impl/dir-contains? dir file))))))))))
