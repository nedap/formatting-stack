(ns formatting-stack.branch-formatter
  "A set of defaults apt for formatting a git branch (namely, the files that a branch has modified, respective to another)."
  (:require
   [formatting-stack.core]
   [formatting-stack.formatters.clean-ns :as formatters.clean-ns]
   [formatting-stack.formatters.cljfmt :as formatters.cljfmt]
   [formatting-stack.formatters.how-to-ns :as formatters.how-to-ns]
   [formatting-stack.formatters.newlines :as formatters.newlines]
   [formatting-stack.formatters.no-extra-blank-lines :as formatters.no-extra-blank-lines]
   [formatting-stack.formatters.trivial-ns-duplicates :as formatters.trivial-ns-duplicates]
   [formatting-stack.indent-specs]
   [formatting-stack.linters.bikeshed :as linters.bikeshed]
   [formatting-stack.linters.eastwood :as linters.eastwood]
   [formatting-stack.linters.kondo :as linters.kondo]
   [formatting-stack.linters.loc-per-ns :as linters.loc-per-ns]
   [formatting-stack.linters.ns-aliases :as linters.ns-aliases]
   [formatting-stack.processors.cider :as processors.cider]
   [formatting-stack.strategies :as strategies]
   [medley.core :refer [mapply]]))

(def third-party-indent-specs formatting-stack.indent-specs/default-third-party-indent-specs)

(defn default-formatters [default-strategies]
  [(formatters.cljfmt/new {:third-party-indent-specs third-party-indent-specs})
   (-> (formatters.how-to-ns/new {})
       (assoc :strategies (conj default-strategies
                                strategies/files-with-a-namespace)))
   (formatters.no-extra-blank-lines/new)
   (formatters.newlines/new {})
   (-> (formatters.trivial-ns-duplicates/new {})
       (assoc :strategies (conj default-strategies
                                strategies/files-with-a-namespace
                                strategies/exclude-edn)))
   (-> (formatters.clean-ns/new {})
       (assoc :strategies (conj default-strategies
                                strategies/files-with-a-namespace
                                strategies/exclude-cljc
                                strategies/exclude-cljs
                                strategies/exclude-edn
                                strategies/namespaces-within-refresh-dirs-only
                                strategies/do-not-use-cached-results!)))])

(defn default-linters [default-strategies]
  [(-> (linters.ns-aliases/new {})
       (assoc :strategies (conj default-strategies
                                strategies/files-with-a-namespace
                                ;; reader conditionals may confuse `linters.ns-aliases`
                                strategies/exclude-cljc
                                ;; string requires may confuse clojure.tools.*
                                strategies/exclude-cljs)))
   (-> (linters.loc-per-ns/new {})
       (assoc :strategies (conj default-strategies
                                strategies/exclude-edn)))
   (-> (linters.bikeshed/new {})
       (assoc :strategies (conj default-strategies
                                strategies/exclude-edn)))
   (-> (linters.eastwood/new {})
       (assoc :strategies (conj default-strategies
                                strategies/exclude-cljs
                                strategies/jvm-requirable-files
                                strategies/namespaces-within-refresh-dirs-only)))
   (-> (linters.kondo/new)
       (assoc :strategies (conj default-strategies
                                strategies/exclude-edn
                                strategies/exclude-clj
                                strategies/exclude-cljc)))])

(def default-processors
  [(processors.cider/new {:third-party-indent-specs third-party-indent-specs})])

(defn format-and-lint-branch! [& {:keys [target-branch in-background?]
                                  :or   {target-branch  "master"
                                         in-background? (not (System/getenv "CI"))}}]
  (let [default-strategies [(fn [& {:as options}]
                              (mapply strategies/git-diff-against-default-branch (assoc options :target-branch target-branch)))]
        formatters (default-formatters default-strategies)
        linters (default-linters default-strategies)]
    (formatting-stack.core/format! :strategies default-strategies
                                   :processors default-processors
                                   :formatters formatters
                                   :linters linters
                                   :in-background? in-background?)))

(defn lint-branch! [& {:keys [target-branch in-background?]
                       :or   {target-branch "master"}}]
  (let [default-strategies [(fn [& {:as options}]
                              (mapply strategies/git-diff-against-default-branch (assoc options :target-branch target-branch)))]
        linters (default-linters default-strategies)]
    (formatting-stack.core/format! :strategies default-strategies
                                   :formatters []
                                   :processors default-processors
                                   :linters linters
                                   :in-background? in-background?)))
