(ns formatting-stack.integrant
  (:require
   [formatting-stack.component.impl :refer [parse-options]]
   [formatting-stack.core :refer [format!]]
   [integrant.core :as integrant]
   [nedap.speced.def :as speced]))

(speced/defn start [^map? this]
  (->> this
       parse-options
       (apply format!))
  this)

(defmethod integrant/init-key ::component [_ this]
  (start this))

(defmethod integrant/halt-key! ::component [_ this]
  this)
