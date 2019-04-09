(ns formatting-stack.branch-formatter
  "A set of defaults apt for formatting a git branch (namely, the files that a branch has modified, respective to another)."
  (:require
   [formatting-stack.core]
   [formatting-stack.formatters.cider :as formatters.cider]
   [formatting-stack.formatters.clean-ns :as formatters.clean-ns]
   [formatting-stack.formatters.cljfmt :as formatters.cljfmt]
   [formatting-stack.formatters.how-to-ns :as formatters.how-to-ns]
   [formatting-stack.formatters.newlines :as formatters.newlines]
   [formatting-stack.indent-specs]
   [formatting-stack.linters.bikeshed :as linters.bikeshed]
   [formatting-stack.linters.eastwood :as linters.eastwood]
   [formatting-stack.strategies :as strategies]
   [medley.core :refer [mapply]]))

(def third-party-indent-specs formatting-stack.indent-specs/default-third-party-indent-specs)

(defn default-formatters [default-strategies]
  (let [opts {:third-party-indent-specs third-party-indent-specs}]
    [(formatters.cider/map->Formatter (assoc opts :strategies default-strategies))
     (formatters.cljfmt/map->Formatter opts)
     (formatters.how-to-ns/map->Formatter opts)
     (formatters.newlines/map->Formatter opts)
     (formatters.clean-ns/map->Formatter (assoc opts :strategies (conj default-strategies
                                                                       strategies/files-with-a-namespace
                                                                       strategies/do-not-use-cached-results!)))]))

(defn default-linters [default-strategies]
  [(linters.bikeshed/map->Bikeshed {:strategies (conj default-strategies
                                                      strategies/exclude-edn)})
   (linters.eastwood/map->Eastwood {:strategies (conj default-strategies
                                                      strategies/exclude-cljs)})])

(defn format-and-lint-branch! [& {:keys [target-branch] :or {target-branch "master"}}]
  (let [default-strategies [(fn [& {:as options}]
                              (mapply strategies/git-diff-against-default-branch (assoc options :target-branch target-branch)))]
        formatters (default-formatters default-strategies)
        linters (default-linters default-strategies)]
    (formatting-stack.core/format! :strategies default-strategies
                                   :formatters formatters
                                   :linters linters
                                   :in-background? false)))
