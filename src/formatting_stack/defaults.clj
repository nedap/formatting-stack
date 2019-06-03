(ns formatting-stack.defaults
  (:require
   [formatting-stack.formatters.cider :as formatters.cider]
   [formatting-stack.formatters.clean-ns :as formatters.clean-ns]
   [formatting-stack.formatters.cljfmt :as formatters.cljfmt]
   [formatting-stack.formatters.how-to-ns :as formatters.how-to-ns]
   [formatting-stack.formatters.newlines :as formatters.newlines]
   [formatting-stack.formatters.no-extra-blank-lines :as formatters.no-extra-blank-lines]
   [formatting-stack.linters.bikeshed :as linters.bikeshed]
   [formatting-stack.linters.eastwood :as linters.eastwood]
   [formatting-stack.linters.kondo :as linters.kondo]
   [formatting-stack.linters.loc-per-ns :as linters.loc-per-ns]
   [formatting-stack.linters.ns-aliases :as linters.ns-aliases]
   [formatting-stack.strategies :as strategies]))

(def default-strategies [strategies/git-completely-staged])

(def extended-strategies [strategies/git-completely-staged
                          strategies/git-not-completely-staged])

(defn default-formatters [third-party-indent-specs]
  (let [opts {:third-party-indent-specs third-party-indent-specs}
        ;; the following exists (for now) to guarantee that how-to-ns uses cached git results from cljfmt.
        ;; ideally the how-to-ns formatter would have an extra `files-with-a-namespace` strategy but that would break git caching,
        ;; making usage more awkward.
        ;; the strategies mechanism needs some rework to avoid this limitation.
        cljfmt-and-how-to-ns-opts (-> opts (assoc :strategies default-strategies))]
    [(formatters.cider/map->Formatter (assoc opts :strategies extended-strategies))
     (formatters.cljfmt/map->Formatter cljfmt-and-how-to-ns-opts)
     (formatters.how-to-ns/map->Formatter cljfmt-and-how-to-ns-opts)
     (formatters.no-extra-blank-lines/map->Formatter {})
     (formatters.newlines/map->Formatter opts)
     (formatters.clean-ns/map->Formatter (assoc opts :strategies (conj default-strategies
                                                                       strategies/files-with-a-namespace
                                                                       strategies/exclude-cljc
                                                                       strategies/exclude-cljs
                                                                       strategies/exclude-edn
                                                                       strategies/do-not-use-cached-results!)))]))

(def default-linters [(linters.ns-aliases/map->Linter {:strategies (conj extended-strategies
                                                                         strategies/files-with-a-namespace
                                                                         ;; reader conditionals may confuse `linters.ns-aliases`
                                                                         strategies/exclude-cljc
                                                                         ;; string requires may confuse clojure.tools.*
                                                                         strategies/exclude-cljs)})
                      (linters.loc-per-ns/map->Linter {:strategies (conj extended-strategies
                                                                         strategies/exclude-edn)})
                      (linters.bikeshed/map->Bikeshed {:strategies (conj extended-strategies
                                                                         strategies/exclude-edn)})
                      (linters.eastwood/map->Eastwood {:strategies (conj extended-strategies
                                                                         strategies/exclude-cljs
                                                                         strategies/jvm-requirable-files)})
                      (linters.kondo/map->Linter {:strategies (conj extended-strategies
                                                                    strategies/exclude-edn
                                                                    strategies/exclude-clj
                                                                    strategies/exclude-cljc)})])

(def default-compilers [])
