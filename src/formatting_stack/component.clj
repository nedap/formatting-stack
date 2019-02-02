(ns formatting-stack.component
  (:require
   [com.stuartsierra.component :as component]
   [formatting-stack.core :refer [format!]]))

(defrecord Formatter [strategies third-party-indent-specs formatters linters]
  component/Lifecycle
  (start [this]
    (format! :strategies strategies
             :third-party-indent-specs third-party-indent-specs
             :formatters formatters
             :linters linters)
    this)

  (stop [this]
    this))
