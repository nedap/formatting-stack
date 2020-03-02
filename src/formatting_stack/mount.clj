(ns formatting-stack.mount
  (:require
   [formatting-stack.component.impl :refer [parse-options]]
   [formatting-stack.core :refer [format!]]
   [mount-up.core :as mount-up]
   [mount.core :as mount]
   [nedap.speced.def :as speced]))

(speced/defn start [^map? this]
  (->> this
       parse-options
       (apply format!))
  this)

(defn configure! [config]
  (mount-up/on-up :formatting (fn [_] (start config)) :before))
