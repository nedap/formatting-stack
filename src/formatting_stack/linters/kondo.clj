(ns formatting-stack.linters.kondo
  (:require
   [formatting-stack.linters.kondo.impl :as impl]
   [nedap.utils.modular.api :refer [implement]]
   [formatting-stack.protocols.linter :as linter]))

(def off {:level :off})

(def default-options
  (list "--config" (pr-str {:linters {:invalid-arity     off
                                      :cond-without-else off}})))

(defn lint! [this filenames]
  (->> filenames
       (cons "--lint")
       (concat default-options)
       (impl/parse-opts)
       impl/lint!))

(defn new []
  (implement {}
    linter/--lint! lint!))
