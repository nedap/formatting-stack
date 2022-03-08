(ns formatting-stack.project-formatter
  "A set of defaults apt for formatting/linting a whole project.

  See also: `formatting-stack.branch-formatter`, `formatting-stack.defaults`"
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
   [formatting-stack.reporters.pretty-printer :as pretty-printer]
   [formatting-stack.strategies :as strategies]))

(def third-party-indent-specs formatting-stack.indent-specs/default-third-party-indent-specs)

(def default-strategies [strategies/all-files])

(def default-reporter
  (pretty-printer/new {}))

(def default-formatters
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

(def default-linters
  [(-> (linters.kondo/new {})
       (assoc :strategies (conj default-strategies
                                strategies/exclude-edn)))
   (-> (linters.one-resource-per-ns/new {})
       (assoc :strategies (conj default-strategies
                                strategies/files-with-a-namespace)))
   (-> (linters.ns-aliases/new {;; When linting an entire project, this option should be false
                                ;; since the user's intent is clearly to not particularly regard the current formatting:
                                :augment-acceptable-aliases-whitelist? false})
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

(def default-processors
  [(processors.cider/new {:third-party-indent-specs third-party-indent-specs})])

(defn format-and-lint-project! [& {:keys [in-background? reporter]
                                   :or   {in-background? false
                                          reporter       default-reporter}}]
  (formatting-stack.core/format! :strategies default-strategies
                                 :formatters default-formatters
                                 :linters default-linters
                                 :reporter reporter
                                 :processors default-processors
                                 :in-background? in-background?))

(defn lint-project! [& {:keys [in-background? reporter]
                        :or   {in-background? false
                               reporter       default-reporter}}]
  (formatting-stack.core/format! :strategies default-strategies
                                 :formatters []
                                 :processors default-processors
                                 :reporter reporter
                                 :linters default-linters
                                 :in-background? in-background?))
