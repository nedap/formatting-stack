(ns formatting-stack.linters.kondo
  (:require
   [clj-kondo.core :as clj-kondo]
   [formatting-stack.protocols.linter :as linter]
   [nedap.utils.modular.api :refer [implement]]))

(def off {:level :off})

(def default-options
  {:linters {:cond-else            off ;; undesired
             :missing-docstring    off ;; undesired
             :unused-symbol        off ;; undesired because unreliable
             :unused-private-var   off ;; undesired because unreliable
             :consistent-alias     off ;; duped by how-to-ns
             :duplicate-require    off ;; duped by clean-ns
             :unused-import        off ;; duped by clean-ns
             :unused-namespace     off ;; duped by clean-ns
             :unused-referred-var  off ;; duped by clean-ns
             :unresolved-namespace off ;; duped by clean-ns
             :misplaced-docstring  off ;; duped by Eastwood
             :deprecated-var       off ;; duped by Eastwood
             :redefined-var        off};; duped by Eastwood
   :lint-as '{clojure.core/bound-fn         clojure.core/fn ;; see https://git.io/JejbK
              nedap.speced.def/def-with-doc clojure.core/defonce
              nedap.speced.def/defn         clojure.core/defn
              nedap.speced.def/defprotocol  clojure.core/defprotocol
              nedap.speced.def/doc          clojure.repl/doc
              nedap.speced.def/fn           clojure.core/fn
              nedap.speced.def/let          clojure.core/let
              nedap.speced.def/letfn        clojure.core/letfn}
   :output {:exclude-files ["test-resources/*"
                            "test/unit/formatting_stack/formatters/cljfmt/impl/sample_data.clj"]}})

(defn lint! [this filenames]
  (-> (clj-kondo/run! {:lint filenames
                       :config default-options})
      (select-keys [:findings])
      clj-kondo/print!))

(defn new []
  (implement {}
    linter/--lint! lint!))
