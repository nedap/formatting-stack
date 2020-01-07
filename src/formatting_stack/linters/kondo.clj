(ns formatting-stack.linters.kondo
  (:require
   [clj-kondo.core :as clj-kondo]
   [formatting-stack.protocols.linter :as linter]
   [medley.core :refer [deep-merge]]
   [nedap.utils.modular.api :refer [implement]]))

(def off {:level :off})

(def default-options
  {:linters {:cond-else            off ;; undesired
             :missing-docstring    off ;; undesired
             :unused-symbol        off ;; can give false positives
             :unused-private-var   off ;; can give false positives
             :consistent-alias     off ;; already offered by how-to-ns
             :duplicate-require    off ;; already offered by clean-ns
             :unused-import        off ;; already offered by clean-ns
             :unused-namespace     off ;; already offered by clean-ns
             :unused-referred-var  off ;; already offered by clean-ns
             :unresolved-namespace off ;; already offered by clean-ns
             }
   :lint-as '{nedap.speced.def/def-with-doc clojure.core/defonce
              nedap.speced.def/defn         clojure.core/defn
              nedap.speced.def/defprotocol  clojure.core/defprotocol
              nedap.speced.def/doc          clojure.repl/doc
              nedap.speced.def/fn           clojure.core/fn
              nedap.speced.def/let          clojure.core/let
              nedap.speced.def/letfn        clojure.core/letfn}
   :output  {:exclude-files ["test-resources/*"
                             "test/unit/formatting_stack/formatters/cljfmt/impl/sample_data.clj"]}})

(def clj-options
  "CLJ files are also linted by eastwood, disable duplicate linters"
  {:linters {:misplaced-docstring off
             :deprecated-var      off
             :redefined-var       off}})

(defn lint! [{:keys [kondo-options]} filenames]
  (let [{cljs-files true
         clj-files  false} (group-by (fn [f] (boolean (re-find #"\.cljs$" f))) filenames)
        findings (->> [(clj-kondo/run! {:lint   clj-files
                                        :config (deep-merge default-options clj-options (or kondo-options {}))})
                       (clj-kondo/run! {:lint   cljs-files
                                        :config (deep-merge default-options (or kondo-options {}))})]
                      (map :findings)
                      (reduce into))]
    (clj-kondo/print! {:findings findings})))

(defn new []
  (implement {}
    linter/--lint! lint!))
