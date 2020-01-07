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
             :misplaced-docstring  off ;; already offered by Eastwood
             :deprecated-var       off ;; already offered by Eastwood
             :redefined-var        off ;; already offered by Eastwood
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

(defn lint! [{:keys [kondo-options]} filenames]
  (-> (clj-kondo/run! {:lint   filenames
                       :config (deep-merge kondo-options default-options)})
      (select-keys [:findings])
      clj-kondo/print!))

(defn new []
  (implement {}
    linter/--lint! lint!))
