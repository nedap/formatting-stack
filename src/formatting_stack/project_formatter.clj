(ns formatting-stack.project-formatter
  "A set of defaults apt for formatting/linting a whole project.

  See also: `formatting-stack.branch-formatter`, `formatting-stack.git-status-formatter`"
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
   [nedap.speced.def :as speced]
   [nedap.utils.modular.api :refer [implement]]))

(def default-reporter
  (pretty-printer/new {}))

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

(defn format-and-lint-project! [& {:keys [in-background? reporter overrides]
                                   :or   {in-background? false
                                          reporter       default-reporter}}]
  (formatting-stack.core/format! :strategies [strategies/all-files]
                                 :overrides overrides
                                 :formatters formatter-factory
                                 :linters linter-factory
                                 :processors processor-factory
                                 :reporter reporter
                                 :in-background? in-background?))

(defn lint-project! [& {:keys [in-background? reporter overrides]
                        :or   {in-background? false
                               reporter       default-reporter}}]
  (formatting-stack.core/format! :strategies [strategies/all-files]
                                 :overrides overrides
                                 :formatters empty-formatter-factory
                                 :processors processor-factory
                                 :reporter reporter
                                 :linters linter-factory
                                 :in-background? in-background?))
