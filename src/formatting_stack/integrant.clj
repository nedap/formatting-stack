(ns formatting-stack.integrant
  (:require
   [formatting-stack.component.impl :refer [parse-options]]
   [formatting-stack.git-status-formatter]
   [integrant.core :as integrant]
   [medley.core :refer [mapply]]
   [nedap.speced.def :as speced]))

(speced/defn start [^map? this]
  (->> this
       parse-options
       (mapply formatting-stack.git-status-formatter/format-and-lint!))
  this)

(defmethod integrant/init-key ::component [_ this]
  (start this))

(defmethod integrant/halt-key! ::component [_ this]
  this)
