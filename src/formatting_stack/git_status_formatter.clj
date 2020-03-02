(ns formatting-stack.git-status-formatter
  "A set of defaults apt for frequent usage XXX.

  See also: `formatting-stack.branch-formatter`, `formatting-stack.project-formatter`"
  (:require
   [formatting-stack.core]
   [formatting-stack.formatters.clean-ns :as formatters.clean-ns]
   [formatting-stack.formatters.cljfmt :as formatters.cljfmt]
   [formatting-stack.formatters.how-to-ns :as formatters.how-to-ns]
   [formatting-stack.formatters.newlines :as formatters.newlines]
   [formatting-stack.formatters.no-extra-blank-lines :as formatters.no-extra-blank-lines]
   [formatting-stack.formatters.trivial-ns-duplicates :as formatters.trivial-ns-duplicates]
   [formatting-stack.linters.eastwood :as linters.eastwood]
   [formatting-stack.linters.kondo :as linters.kondo]
   [formatting-stack.linters.line-length :as linters.line-length]
   [formatting-stack.linters.loc-per-ns :as linters.loc-per-ns]
   [formatting-stack.linters.ns-aliases :as linters.ns-aliases]
   [formatting-stack.linters.one-resource-per-ns :as linters.one-resource-per-ns]
   [formatting-stack.processors.cider :as processors.cider]
   [formatting-stack.reporters.pretty-printer :as pretty-printer]
   [formatting-stack.strategies :as strategies]
   [nedap.speced.def :as speced]))

(speced/defn default-formatters [^vector? formatters-strategies
                                 ^map? third-party-indent-specs]
  (let [;; the following exists (for now) to guarantee that how-to-ns uses cached git results from cljfmt.
        ;; ideally the how-to-ns formatter would have an extra `files-with-a-namespace` strategy but that would break git caching,
        ;; making usage more awkward.
        ;; the strategies mechanism needs some rework to avoid this limitation.
        cached-strategies formatters-strategies]
    (->> [(-> (formatters.cljfmt/new {:third-party-indent-specs third-party-indent-specs})
              (assoc :strategies cached-strategies))
          (-> (formatters.how-to-ns/new {})
              (assoc :strategies cached-strategies))
          (formatters.no-extra-blank-lines/new)
          (formatters.newlines/new {})
          (-> (formatters.trivial-ns-duplicates/new {})
              (assoc :strategies (conj formatters-strategies
                                       strategies/files-with-a-namespace
                                       strategies/exclude-edn)))
          (when (strategies/refactor-nrepl-available?)
            (-> (formatters.clean-ns/new {})
                (assoc :strategies (conj formatters-strategies
                                         strategies/files-with-a-namespace
                                         strategies/exclude-cljc
                                         strategies/exclude-cljs
                                         strategies/exclude-edn
                                         strategies/namespaces-within-refresh-dirs-only
                                         strategies/do-not-use-cached-results!))))]
         (filterv some?))))

(speced/defn default-linters [^vector? linters-strategies]
  [(-> (linters.kondo/new {})
       (assoc :strategies (conj linters-strategies
                                strategies/exclude-edn)))
   (-> (linters.one-resource-per-ns/new {})
       (assoc :strategies (conj linters-strategies
                                strategies/files-with-a-namespace)))
   (-> (linters.ns-aliases/new {})
       (assoc :strategies (conj linters-strategies
                                strategies/files-with-a-namespace
                                ;; reader conditionals may confuse `linters.ns-aliases`
                                strategies/exclude-cljc
                                ;; string requires may confuse clojure.tools.*
                                strategies/exclude-cljs)))
   (-> (linters.line-length/new {})
       (assoc :strategies (conj linters-strategies
                                strategies/exclude-edn)))
   (-> (linters.loc-per-ns/new {})
       (assoc :strategies (conj linters-strategies
                                strategies/exclude-edn)))
   (-> (linters.eastwood/new {})
       (assoc :strategies (conj linters-strategies
                                strategies/exclude-cljs
                                strategies/jvm-requirable-files
                                strategies/namespaces-within-refresh-dirs-only)))])

(speced/defn default-processors [^vector? processors-strategies, ^map? third-party-indent-specs]
  [(-> (processors.cider/new {:third-party-indent-specs third-party-indent-specs})
       (assoc :strategies processors-strategies))])

(def default-reporter
  (pretty-printer/new {}))

(defn format-and-lint! [& {:keys [in-background? reporter]
                           :or   {in-background? false
                                  reporter       default-reporter}}]
  (formatting-stack.core/format! :strategies [strategies/git-completely-staged]
                                 :overrides {:strategies {:linters [strategies/git-not-completely-staged]}}
                                 :formatters default-formatters
                                 :linters default-linters
                                 :processors default-processors
                                 :reporter reporter
                                 :in-background? in-background?))

(defn lint! [& {:keys [in-background? reporter]
                :or   {in-background? false
                       reporter       default-reporter}}]
  (formatting-stack.core/format! :strategies [strategies/git-completely-staged
                                              strategies/git-not-completely-staged]
                                 :formatters (constantly [])
                                 :processors default-processors
                                 :reporter reporter
                                 :linters default-linters
                                 :in-background? in-background?))
