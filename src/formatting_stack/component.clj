(ns formatting-stack.component
  (:require
   [com.stuartsierra.component :as component]
   [formatting-stack.core :refer [format!]]))

(defrecord Formatter [strategies third-party-indent-specs formatters linters processors in-background?]
  component/Lifecycle
  (start [this]
    (format! :strategies strategies
             :third-party-indent-specs third-party-indent-specs
             :formatters formatters
             :linters linters
             :processors processors
             :in-background? in-background?)
    this)

  (stop [this]
    this))
