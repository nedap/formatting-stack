(ns formatting-stack.linters.kondo
  (:require
   [clj-kondo.core :as clj-kondo]
   [formatting-stack.protocols.linter :as linter]
   [nedap.utils.modular.api :refer [implement]]))

(def off {:level :off})

(def default-options
  {:linters {:invalid-arity     off
             :cond-without-else off}})

(defn lint! [this filenames]
  (-> (clj-kondo/run! {:lint filenames
                       :config default-options})
      (select-keys [:findings])
      clj-kondo/print!))

(defn new []
  (implement {}
    linter/--lint! lint!))
