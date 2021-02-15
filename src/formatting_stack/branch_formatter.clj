(ns formatting-stack.branch-formatter
  "A set of defaults apt for formatting/linting a git branch
  (namely, the files that a branch has modified, respective to another).

  See also: `formatting-stack.git-status-formatter`, `formatting-stack.project-formatter`"
  (:require
   [formatting-stack.core]
   [formatting-stack.formatters.clean-ns :as formatters.clean-ns]
   [formatting-stack.formatters.cljfmt :as formatters.cljfmt]
   [formatting-stack.formatters.how-to-ns :as formatters.how-to-ns]
   [formatting-stack.formatters.newlines :as formatters.newlines]
   [formatting-stack.formatters.no-extra-blank-lines :as formatters.no-extra-blank-lines]
   [formatting-stack.formatters.trivial-ns-duplicates :as formatters.trivial-ns-duplicates]
   [formatting-stack.indent-specs]
   [formatting-stack.linters.eastwood :as linters.eastwood]
   [formatting-stack.linters.kondo :as linters.kondo]
   [formatting-stack.linters.line-length :as linters.line-length]
   [formatting-stack.linters.loc-per-ns :as linters.loc-per-ns]
   [formatting-stack.linters.ns-aliases :as linters.ns-aliases]
   [formatting-stack.linters.one-resource-per-ns :as linters.one-resource-per-ns]
   [formatting-stack.processors.cider :as processors.cider]
   [formatting-stack.protocols.formatter :as protocols.formatter]
   [formatting-stack.protocols.linter :as protocols.linter]
   [formatting-stack.protocols.processor :as protocols.processor]
   [formatting-stack.reporters.pretty-printer :as pretty-printer]
   [formatting-stack.strategies :as strategies]
   [medley.core :refer [mapply]]
   [nedap.speced.def :as speced]
   [nedap.utils.modular.api :refer [implement]]))

(speced/defn default-formatters [_
                                 ^vector? default-strategies
                                 ^map? third-party-indent-specs]
  (->> [(formatters.cljfmt/new {:third-party-indent-specs third-party-indent-specs})
        (-> (formatters.how-to-ns/new {})
            (assoc :strategies (conj default-strategies
                                     strategies/files-with-a-namespace)))
        (formatters.no-extra-blank-lines/new)
        (formatters.newlines/new {})
        (-> (formatters.trivial-ns-duplicates/new {})
            (assoc :strategies (conj default-strategies
                                     strategies/files-with-a-namespace
                                     strategies/exclude-edn)))
        (when (strategies/refactor-nrepl-available?)
          (-> (formatters.clean-ns/new {})
              (assoc :strategies (conj default-strategies
                                       strategies/when-refactor-nrepl
                                       strategies/files-with-a-namespace
                                       strategies/exclude-cljc
                                       strategies/exclude-cljs
                                       strategies/exclude-edn
                                       strategies/namespaces-within-refresh-dirs-only
                                       strategies/do-not-use-cached-results!))))]

       (filterv some?)))

(defn default-linters [_
                       default-strategies]
  [(-> (linters.kondo/new {})
       (assoc :strategies (conj default-strategies
                                strategies/exclude-edn)))
   (-> (linters.one-resource-per-ns/new {})
       (assoc :strategies (conj default-strategies
                                strategies/files-with-a-namespace)))
   (-> (linters.ns-aliases/new {})
       (assoc :strategies (conj default-strategies
                                strategies/files-with-a-namespace
                                ;; reader conditionals may confuse `linters.ns-aliases`
                                strategies/exclude-cljc
                                ;; string requires may confuse clojure.tools.*
                                strategies/exclude-cljs)))
   (-> (linters.line-length/new {})
       (assoc :strategies (conj default-strategies
                                strategies/exclude-edn)))
   (-> (linters.loc-per-ns/new {})
       (assoc :strategies (conj default-strategies
                                strategies/exclude-edn)))
   (-> (linters.eastwood/new {})
       (assoc :strategies (conj default-strategies
                                strategies/exclude-cljs
                                strategies/jvm-requirable-files
                                strategies/namespaces-within-refresh-dirs-only)))])

(speced/defn default-processors [_
                                 ^vector? processors-strategies
                                 ^map? third-party-indent-specs]
  [(processors.cider/new {:third-party-indent-specs third-party-indent-specs})])

(def default-reporter
  (pretty-printer/new {}))

(def formatter-factory
  (implement {}
    protocols.formatter/--formatters default-formatters))

(def no-formatters (constantly []))

(def empty-formatter-factory
  (implement {}
    protocols.formatter/--formatters no-formatters))

(def linter-factory
  (implement {}
    protocols.linter/--linters default-linters))

(def processor-factory
  (implement {}
    protocols.processor/--processors default-processors))

(defn format-and-lint-branch!
  "Note that files that are not completely staged will not be affected.

  You can override that with `:blacklist []`."
  [& {:keys [target-branch in-background? reporter formatters blacklist overrides]
      :or   {target-branch  "master"
             in-background? (not (System/getenv "CI"))
             reporter       default-reporter}}]
  (let [default-strategies [(fn [& {:as options}]
                              (mapply strategies/git-diff-against-default-branch (cond-> options
                                                                                   true      (assoc :target-branch target-branch)
                                                                                   blacklist (assoc :blacklist blacklist))))]]
    (formatting-stack.core/format! :strategies default-strategies
                                   :overrides overrides
                                   :formatters (or formatters formatter-factory)
                                   :linters linter-factory
                                   :processors processor-factory
                                   :reporter reporter
                                   :in-background? in-background?)))

(defn lint-branch! [& {:keys [target-branch in-background? reporter overrides]
                       :or   {target-branch  "master"
                              in-background? false
                              reporter       default-reporter}}]
  (format-and-lint-branch! :target-branch target-branch
                           :overrides overrides
                           :in-background? in-background?
                           :reporter reporter
                           :formatters empty-formatter-factory
                           ;; analyze a broader selection of files:
                           :blacklist []))
