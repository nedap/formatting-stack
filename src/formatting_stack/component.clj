(ns formatting-stack.component
  (:require
   [com.stuartsierra.component :as component]
   [formatting-stack.component.impl :refer [parse-options]]
   [formatting-stack.core :refer [format!]]
   [nedap.speced.def :as speced]
   [nedap.utils.modular.api :refer [implement]]))

(speced/defn start [^map? this]
  (->> this
       parse-options
       (apply format!))
  this)

(defn new
  "Accepts a map of options to be passed to `formatting-stack.core/format!`.

  Returns a Component that invokes `#'format!` with said options on each `#'component/start` invocation."
  [this]
  (implement this
    component/start start))
