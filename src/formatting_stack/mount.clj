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

;; Allow parameterless configuring using `mount/start-with-args`.
(mount-up/on-up :formatting-with-mount-args
                (fn [_]
                  (when-let [config (::config (mount/args))]
                    (start config)))
                :before)

(defn configure! [config]
  (mount-up/on-up :formatting-with-provided-config
                  (fn [_]
                    (start config))
                  :before))
