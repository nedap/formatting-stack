(ns formatting-stack.branch-formatter
  "A set of defaults apt for formatting a git branch (namely, the files that a branch has modified, respective to another)."
  (:require
   [clojure.string :as str]
   [formatting-stack.core]
   [formatting-stack.formatters.cider :as formatters.cider]
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
   [formatting-stack.strategies :as strategies]
   [medley.core :refer [mapply]]
   [nedap.speced.def :as speced])
  (:gen-class))

(def third-party-indent-specs formatting-stack.indent-specs/default-third-party-indent-specs)

(defn default-formatters [default-strategies]
  (let [opts {:third-party-indent-specs third-party-indent-specs}]
    [(formatters.cider/map->Formatter (assoc opts :strategies default-strategies))
     (formatters.cljfmt/map->Formatter opts)
     (formatters.how-to-ns/map->Formatter (assoc opts :strategies (conj default-strategies
                                                                        strategies/files-with-a-namespace)))
     (formatters.no-extra-blank-lines/map->Formatter {})
     (formatters.newlines/map->Formatter opts)
     (formatters.trivial-ns-duplicates/map->Formatter (assoc opts :strategies (conj default-strategies
                                                                                    strategies/files-with-a-namespace
                                                                                    strategies/exclude-edn)))
     (formatters.clean-ns/map->Formatter (assoc opts :strategies (conj default-strategies
                                                                       strategies/files-with-a-namespace
                                                                       strategies/exclude-cljc
                                                                       strategies/exclude-cljs
                                                                       strategies/exclude-edn
                                                                       strategies/namespaces-within-refresh-dirs-only
                                                                       strategies/do-not-use-cached-results!)))]))

(defn default-linters [default-strategies]
  [(linters.ns-aliases/map->Linter {:strategies (conj default-strategies
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
                                                      strategies/exclude-cljs
                                                      strategies/jvm-requirable-files
                                                      strategies/namespaces-within-refresh-dirs-only)})
   (linters.kondo/map->Linter {:strategies (conj default-strategies
                                                 strategies/exclude-edn
                                                 strategies/exclude-clj
                                                 strategies/exclude-cljc)})])

(defn format-and-lint-branch! [& {:keys [target-branch in-background?]
                                  :or   {target-branch  "master"
                                         in-background? (not (System/getenv "CI"))}}]
  (let [default-strategies [(fn [& {:as options}]
                              (mapply strategies/git-diff-against-default-branch (assoc options :target-branch target-branch)))]
        formatters (default-formatters default-strategies)
        linters (default-linters default-strategies)]
    (formatting-stack.core/format! :strategies default-strategies
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
                                   :compilers []
                                   :linters linters
                                   :in-background? in-background?)))

(speced/defn -main [command & _args]
  (assert (#{"pristine?"} command)
          (str "Command not recognised.\n\n"
               "Available commands: pristine?.\n\n"
               "You can refer to the documentation in: https://github.com/nedap/formatting-stack"))

  (let [default-strategies [(fn [& {:as options}]
                              (mapply strategies/git-diff-against-default-branch (assoc options :target-branch "master")))]
        opts               {:third-party-indent-specs third-party-indent-specs}
        linters            [(formatters.cljfmt/map->Formatter opts)
                            (formatters.clean-ns/map->Formatter (assoc opts :strategies (conj default-strategies
                                                                                              strategies/files-with-a-namespace
                                                                                              strategies/exclude-cljc
                                                                                              strategies/exclude-cljs
                                                                                              strategies/exclude-edn
                                                                                              strategies/do-not-use-cached-results!)))]
        results            (formatting-stack.core/process! formatting-stack.protocols.linter/lint!
                                                           linters
                                                           nil
                                                           default-strategies
                                                           false)

        print-results (fn [results] (->> (zipmap (map type linters) results)
                                         (reduce-kv (fn reduce-linters [memo linter filenames]
                                                      (reduce (fn reduce-files [memo filename]
                                                                  (update memo filename (fnil conj []) linter))
                                                        memo
                                                        filenames))
                                                    {})
                                         (run! (fn print-violations [[filename linters]]
                                                 (println (str "📄" "\033[1m ./" filename "\033[0m\n"
                                                               (->> linters
                                                                    (map #(str " - " (pr-str %) "\n"))
                                                                    (apply str))))))))]
    (when-not (every? empty? results)
      (println "🚨 Violations found: \n")
      (print-results results)
      (System/exit 1)))

  (println "👍🏻 No violations found"))
