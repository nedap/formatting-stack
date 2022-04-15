(ns formatting-stack.strategies
  "Strategies are concerned with generating a seq of filenames to process (format, lint, or compile).

  Said filenames must satisfy the `::protocols.spec/filename` spec, and represent an existing file.

  They are configured to run in a determinate order.

  In practice, a strategy is function that receives a seq of filenames, and returns another:

  * more filenames may be added; and or
  * the passed filenames may be `filter`ed.

  A strategy may not return nil."
  (:require
   [clojure.java.shell :refer [sh]]
   [clojure.string :as string]
   [clojure.tools.namespace.repl :as tools.namespace.repl]
   [formatting-stack.protocols.spec :as protocols.spec]
   [formatting-stack.strategies.impl :as impl]
   [formatting-stack.strategies.impl.git-status :as git-status]
   [formatting-stack.util :refer [read-ns-decl require-lock try-require]]
   [nedap.speced.def :as speced]
   [nedap.utils.collections.eager :refer [partitioning-pmap]]
   [nedap.utils.spec.api :refer [check!]])
  (:import
   (java.io File)))

(def git-command "git")

(speced/defn all-files
  "This strategy unconditionally processes all Clojure and ClojureScript files."
  [& {:keys [^::protocols.spec/filenames files]}]
  ;; This first `binding` is necessary for obtaining an absolutized list of deletions
  (binding [impl/*skip-existing-files-check?* true]
    (let [deleted (->> (impl/file-entries git-command "status" "--porcelain")
                       (filter git-status/deleted-file?)
                       (map git-status/remove-deletion-markers)
                       (impl/absolutize git-command)
                       (set))
          ;; DRY the use of the `--full-name` option which is especially important:
          ls-files (speced/fn [^list? args]
                     (->> args
                          (cons "--full-name")
                          (cons "ls-files")
                          (apply impl/file-entries git-command)
                          (impl/absolutize git-command)
                          (remove deleted)))
          tracked (ls-files ())
          untracked (ls-files '("--others" "--exclude-standard"))]
      ;; Second `binding`, to ensure correct results
      (binding [impl/*skip-existing-files-check?* false]
        (speced/let [^::impl/existing-files corpus (into tracked untracked)]
          (->> files
               (into corpus)
               (impl/extract-clj-files)))))))

(speced/defn git-completely-staged
  "This strategy processes the new or modified files that are _completely_ staged with git."
  [& {:keys [^::protocols.spec/filenames files, impl]
      :or   {impl (impl/file-entries git-command "status" "--porcelain")}}]
  (->> impl
       (filter #(re-find impl/git-completely-staged-regex %))
       (remove git-status/deleted-file?)
       (map #(string/replace-first % impl/git-completely-staged-regex ""))
       (map (fn [s]
              ;; for renames:
              (-> s (string/split #" -> ") last)))
       (impl/absolutize git-command)
       (impl/extract-clj-files)
       (into files)))

(speced/defn git-not-completely-staged
  "This strategy processes all files that are not _completely_ staged with git. Untracked files are also included."
  [& {:keys [^::protocols.spec/filenames files, impl]
      :or   {impl (impl/file-entries git-command "status" "--porcelain")}}]
  (->> impl
       (remove git-status/deleted-file?)
       (filter #(re-find impl/git-not-completely-staged-regex %))
       (map #(string/replace-first % impl/git-not-completely-staged-regex ""))
       (impl/absolutize git-command)
       (impl/extract-clj-files)
       (into files)))

(defn current-branch-name []
  (-> (sh "git" "rev-parse" "--abbrev-ref" "HEAD")
      (:out)
      (string/split-lines)
      (first)))

(defn default-branch-name []
  (let [fallback-property-name "formatting-stack.default-branch-name"
        fallback-branch-name "master"]
    (or (not-empty (System/getProperty fallback-property-name))
        (let [all-branches (->> (sh "git" "branch")
                                :out
                                string/split-lines
                                (map (fn [s]
                                       (-> s (string/split #"\s+") last)))
                                (set))]
          (or (some all-branches ["master" "main" "stable" "dev"])
              (do
                (println (format "No default branch could be determined. Falling back to `%s`.
You can choose another one by setting the `%s` system property." fallback-branch-name fallback-property-name))
                ;; return something, for not breaking code that traditionally assumed "master":
                fallback-branch-name))))))

(defn git-diff-against-default-branch
  "This strategy processes all files that this branch has modified.
  The diff is compared against the `:target-branch` option."
  [& {:keys [target-branch impl files blacklist]
      :or   {target-branch (default-branch-name)
             ;; We filter for Added, Copied, Modified and Renamed files,
             ;; excluding Unmerged, Deleted, Type-changed, Broken (pair), and Unknown files
             impl          (impl/file-entries git-command "diff" "--name-only" "--diff-filter=ACMR" target-branch "--")
             blacklist     (git-not-completely-staged :files [])}}]
  (assert (impl/git-ref-exists? target-branch)
          (str (pr-str target-branch) " was not recognised as an existing git branch, tag or commit sha."))
  (->> impl
       (impl/absolutize git-command)
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
  [& {:keys [^::protocols.spec/filenames files
             refresh-dirs]
      :or {refresh-dirs tools.namespace.repl/refresh-dirs}}]
  {:pre [(check! seq                            refresh-dirs
                 (partial every? (speced/fn [^string? refresh-dir]
                                   (let [file (-> refresh-dir File.)]
                                     (if (-> file .exists)
                                       (-> file .isDirectory)
                                       ;; allow non-existing directories.
                                       ;; Temporary, until https://git.io/Jeaah is a thing:
                                       true)))) refresh-dirs)]}
  (->> files
       (partitioning-pmap (speced/fn [^string? filename]
                            (if-not (read-ns-decl filename)
                              filename
                              (let [file (-> filename File.)]
                                (when (->> refresh-dirs
                                           (some (fn [dir]
                                                   (impl/dir-contains? dir file))))
                                  filename)))))
       (filter identity)))

(defn refactor-nrepl-available? []
  (locking require-lock
    (try
      (require 'refactor-nrepl.ns.clean-ns)
      true
      (catch Throwable _
        false))))

(defn refactor-nrepl-3-4-1-available? []
  (locking require-lock
    (try
      (requiring-resolve 'refactor-nrepl.ns.libspecs/namespace-aliases-for)
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
