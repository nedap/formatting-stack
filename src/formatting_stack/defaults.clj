(ns formatting-stack.defaults
  (:require
   [formatting-stack.formatters.cider :as formatters.cider]
   [formatting-stack.formatters.clean-ns :as formatters.clean-ns]
   [formatting-stack.formatters.cljfmt :as formatters.cljfmt]
   [formatting-stack.formatters.how-to-ns :as formatters.how-to-ns]
   [formatting-stack.formatters.newlines :as formatters.newlines]
   [formatting-stack.linters.bikeshed :as linters.bikeshed]
   [formatting-stack.linters.eastwood :as linters.eastwood]
   [formatting-stack.strategies :as strategies]))

(def default-strategies [strategies/git-completely-staged])

(def extended-strategies [strategies/git-completely-staged
                          strategies/git-not-completely-staged])

(defn default-formatters [third-party-indent-specs]
  (let [opts {:third-party-indent-specs third-party-indent-specs}]
    [(formatters.cider/map->Formatter (assoc opts :strategies extended-strategies))
     (formatters.cljfmt/map->Formatter opts)
     (formatters.how-to-ns/map->Formatter opts)
     (formatters.newlines/map->Formatter opts)
     (formatters.clean-ns/map->Formatter (assoc opts :strategies (conj default-strategies
                                                                       strategies/do-not-use-cached-results!)))]))

(def default-linters [(linters.bikeshed/map->Bikeshed {:strategies extended-strategies})
                      (linters.eastwood/map->Eastwood {:strategies extended-strategies})])

(def default-compilers [])
