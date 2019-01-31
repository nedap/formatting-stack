(ns formatting-stack.component
  (:require
   [com.stuartsierra.component :as component]
   [formatting-stack.core :refer [format!]]))

(defrecord Formatter [strategies third-party-indent-specs formatters]
  component/Lifecycle
  (start [this]
    (format! :strategies strategies
             :third-party-indent-specs third-party-indent-specs
             :formatters formatters)
    this)

  (stop [this]
    this))
