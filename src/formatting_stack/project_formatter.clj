(ns formatting-stack.project-formatter
  "A set of defaults apt for formatting a whole project."
  (:require
   [formatting-stack.core]
   [formatting-stack.formatters.cider :as formatters.cider]
   [formatting-stack.formatters.clean-ns :as formatters.clean-ns]
   [formatting-stack.formatters.cljfmt :as formatters.cljfmt]
   [formatting-stack.formatters.how-to-ns :as formatters.how-to-ns]
   [formatting-stack.formatters.newlines :as formatters.newlines]
   [formatting-stack.formatters.no-extra-blank-lines :as formatters.no-extra-blank-lines]
   [formatting-stack.indent-specs]
   [formatting-stack.linters.bikeshed :as linters.bikeshed]
   [formatting-stack.linters.eastwood :as linters.eastwood]
   [formatting-stack.linters.loc-per-ns :as linters.loc-per-ns]
   [formatting-stack.linters.ns-aliases :as linters.ns-aliases]
   [formatting-stack.strategies :as strategies]))

(def third-party-indent-specs formatting-stack.indent-specs/default-third-party-indent-specs)

(def default-strategies [strategies/all-files])

(def default-formatters
  (let [opts {:third-party-indent-specs third-party-indent-specs}]
    [(formatters.cider/map->Formatter {})
     (formatters.cljfmt/map->Formatter opts)
     (formatters.how-to-ns/map->Formatter (-> opts (assoc :strategies (conj default-strategies
                                                                            strategies/files-with-a-namespace
                                                                            strategies/exclude-cljc))))
     (formatters.no-extra-blank-lines/map->Formatter {})
     (formatters.newlines/map->Formatter opts)
     (formatters.clean-ns/map->Formatter (-> opts (assoc :strategies (conj default-strategies
                                                                           strategies/files-with-a-namespace
                                                                           strategies/exclude-cljs
                                                                           strategies/exclude-edn
                                                                           strategies/do-not-use-cached-results!))))]))

(def default-linters [(linters.ns-aliases/map->Linter {:strategies (conj default-strategies
                                                                         strategies/files-with-a-namespace
                                                                         ;; reader conditionals may confuse `linters.ns-aliases`
                                                                         strategies/exclude-cljc
                                                                         ;; string requires may confuse clojure.tools.*
                                                                         strategies/exclude-cljs)})
                      (linters.loc-per-ns/map->Linter {:strategies (conj default-strategies
                                                                         strategies/exclude-edn)})
                      (linters.bikeshed/map->Bikeshed {:strategies (conj default-strategies
                                                                         strategies/exclude-edn)})
                      (linters.eastwood/map->Eastwood {:strategies (conj default-strategies
                                                                         strategies/exclude-cljs)})])

(def default-compilers [])

(defn format-and-lint-project! [& {:keys [in-background?]}]
  (formatting-stack.core/format! :strategies default-strategies
                                 :formatters default-formatters
                                 :linters default-linters
                                 :in-background? in-background?
                                 :intersperse-newlines? true))

(defn lint-project! [& {:keys [in-background?]}]
  (formatting-stack.core/format! :strategies default-strategies
                                 :formatters []
                                 :compilers []
                                 :linters default-linters
                                 :in-background? in-background?
                                 :intersperse-newlines? true))