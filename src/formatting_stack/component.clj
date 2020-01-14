(ns formatting-stack.component
  (:require
   [com.stuartsierra.component :as component]
   [formatting-stack.core :refer [format!]]
   [nedap.utils.modular.api :refer [implement]]))

(defn start [this]
  (apply format! (mapcat identity this))
  this)

(defn new [this]
  (implement this
    component/start start))
