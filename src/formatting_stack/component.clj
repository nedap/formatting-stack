(ns formatting-stack.component
  (:require
   [com.stuartsierra.component :as component]
   [formatting-stack.core :refer [format!]]))

(defrecord Formatter [strategies third-party-indent-specs formatters linters compilers]
  component/Lifecycle
  (start [this]
    (future ;; don't delay system initialization
      (format! :strategies strategies
               :third-party-indent-specs third-party-indent-specs
               :formatters formatters
               :linters linters
               :compilers compilers))
    this)

  (stop [this]
    this))
