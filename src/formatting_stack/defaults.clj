(ns formatting-stack.defaults
  "A set of defaults apt for frequent usage.

  See also: `formatting-stack.branch-formatter`, `formatting-stack.project-formatter`"
  (:require
   [formatting-stack.formatters.clean-ns :as formatters.clean-ns]
   [formatting-stack.formatters.cljfmt :as formatters.cljfmt]
   [formatting-stack.formatters.how-to-ns :as formatters.how-to-ns]
   [formatting-stack.formatters.newlines :as formatters.newlines]
   [formatting-stack.formatters.no-extra-blank-lines :as formatters.no-extra-blank-lines]
   [formatting-stack.formatters.trivial-ns-duplicates :as formatters.trivial-ns-duplicates]
   [formatting-stack.linters.eastwood :as linters.eastwood]
   [formatting-stack.linters.line-length :as linters.line-length]
   [formatting-stack.linters.loc-per-ns :as linters.loc-per-ns]
   [formatting-stack.linters.ns-aliases :as linters.ns-aliases]
   [formatting-stack.linters.one-resource-per-ns :as linters.one-resource-per-ns]
   [formatting-stack.processors.cider :as processors.cider]
   [formatting-stack.strategies :as strategies]))

(def default-strategies [strategies/git-completely-staged])

(def extended-strategies [strategies/git-completely-staged
                          strategies/git-not-completely-staged])

(defn default-formatters [third-party-indent-specs]
  (let [;; the following exists (for now) to guarantee that how-to-ns uses cached git results from cljfmt.
        ;; ideally the how-to-ns formatter would have an extra `files-with-a-namespace` strategy but that would break git caching,
        ;; making usage more awkward.
        ;; the strategies mechanism needs some rework to avoid this limitation.
        cached-strategies default-strategies]
    (->> [(-> (formatters.cljfmt/new {:third-party-indent-specs third-party-indent-specs})
              (assoc :strategies cached-strategies))
          (-> (formatters.how-to-ns/new {})
              (assoc :strategies cached-strategies))
          (formatters.no-extra-blank-lines/new)
          (formatters.newlines/new {})
          (-> (formatters.trivial-ns-duplicates/new {})
              (assoc :strategies (conj default-strategies
                                       strategies/files-with-a-namespace
                                       strategies/exclude-edn)))
          (when (strategies/refactor-nrepl-available?)
            (-> (formatters.clean-ns/new {})
                (assoc :strategies (conj default-strategies
                                         strategies/files-with-a-namespace
                                         strategies/exclude-cljc
                                         strategies/exclude-cljs
                                         strategies/exclude-edn
                                         strategies/namespaces-within-refresh-dirs-only
                                         strategies/do-not-use-cached-results!))))]
         (filterv some?))))

(def default-linters [(-> (linters.one-resource-per-ns/new {})
                          (assoc :strategies (conj extended-strategies
                                                   strategies/files-with-a-namespace)))
                      (-> (linters.ns-aliases/new {})
                          (assoc :strategies (conj extended-strategies
                                                   strategies/files-with-a-namespace
                                                   ;; reader conditionals may confuse `linters.ns-aliases`
                                                   strategies/exclude-cljc
                                                   ;; string requires may confuse clojure.tools.*
                                                   strategies/exclude-cljs)))
                      (-> (linters.line-length/new {})
                          (assoc :strategies (conj extended-strategies
                                                   strategies/exclude-edn)))
                      (-> (linters.loc-per-ns/new {})
                          (assoc :strategies (conj extended-strategies
                                                   strategies/exclude-edn)))
                      (-> (linters.eastwood/new {})
                          (assoc :strategies (conj extended-strategies
                                                   strategies/exclude-cljs
                                                   strategies/jvm-requirable-files
                                                   strategies/namespaces-within-refresh-dirs-only)))])

(defn default-processors [third-party-indent-specs]
  [(-> (processors.cider/new {:third-party-indent-specs third-party-indent-specs})
       (assoc :strategies extended-strategies))])
