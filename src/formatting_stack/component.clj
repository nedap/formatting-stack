(ns formatting-stack.component
  (:require
   [com.stuartsierra.component :as component]
   [formatting-stack.core :refer [format!]]
   [nedap.utils.modular.api :refer [implement]]))

(defn start [this]
  (apply format! (mapcat identity this))
  this)

(defn new
  [{:keys [strategies third-party-indent-specs formatters linters processors in-background?] :as this}]
  (implement this
    component/start start))
