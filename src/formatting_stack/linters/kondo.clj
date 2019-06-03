(ns formatting-stack.linters.kondo
  (:require
   [formatting-stack.linters.kondo.impl :as impl]
   [formatting-stack.protocols.linter]))

(def off {:level :off})

(def default-options
  (list "--config" (pr-str {:linters {:invalid-arity     off
                                      :cond-without-else off}})))

(defrecord Linter []
  formatting-stack.protocols.linter/Linter
  (lint! [this filenames]
    (->> filenames
         (cons "--lint")
         (concat default-options)
         (impl/parse-opts)
         impl/lint!)))
