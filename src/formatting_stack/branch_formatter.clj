(ns formatting-stack.branch-formatter
  "A set of defaults apt for formatting/linting a git branch
  (namely, the files that a branch has modified, respective to another).

  See also: `formatting-stack.defaults`, `formatting-stack.project-formatter`"
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
   [formatting-stack.strategies :as strategies]
   [medley.core :refer [mapply]]))

(def third-party-indent-specs formatting-stack.indent-specs/default-third-party-indent-specs)

(defn default-formatters [default-strategies]
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

(defn default-linters [default-strategies]
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

(def default-processors
  [(processors.cider/new {:third-party-indent-specs third-party-indent-specs})])

(def default-reporter
  (pretty-printer/new {}))

(defn format-and-lint-branch! [& {:keys [target-branch in-background? reporter]
                                  :or   {target-branch  "master"
                                         in-background? (not (System/getenv "CI"))
                                         reporter       default-reporter}}]
  (let [default-strategies [(fn [& {:as options}]
                              (mapply strategies/git-diff-against-default-branch (assoc options :target-branch target-branch)))]
        formatters (default-formatters default-strategies)
        linters (default-linters default-strategies)]
    (formatting-stack.core/format! :strategies default-strategies
                                   :processors default-processors
                                   :formatters formatters
                                   :reporter reporter
                                   :linters linters
                                   :in-background? in-background?)))

(defn lint-branch! [& {:keys [target-branch in-background? reporter]
                       :or   {target-branch  "master"
                              in-background? false
                              reporter       default-reporter}}]
  (let [default-strategies [(fn [& {:as options}]
                              (mapply strategies/git-diff-against-default-branch (assoc options :target-branch target-branch)))]
        linters (default-linters default-strategies)]
    (formatting-stack.core/format! :strategies default-strategies
                                   :formatters []
                                   :processors default-processors
                                   :reporter reporter
                                   :linters linters
                                   :in-background? in-background?)))
