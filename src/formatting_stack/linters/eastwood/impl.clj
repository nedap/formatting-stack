(ns formatting-stack.linters.eastwood.impl
  (:require
   [clojure.string :as string]
   [eastwood.reporting-callbacks :as reporting-callbacks]
   [formatting-stack.protocols.spec :as protocols.spec]
   [nedap.speced.def :as speced]))

(speced/defn ^::protocols.spec/reports exceptions->reports
  [exceptions]
  (->> exceptions
       (map (fn [exception]
              {:level     :exception
               :source    :formatting-stack/report-processing-error
               :msg       (str "Encountered an exception while running Eastwood")
               :exception exception}))))

(def vconj (fnil conj []))

(defrecord TrackingReporter [reports])

(defmethod reporting-callbacks/lint-warning TrackingReporter [{:keys [reports]} warning]
  (swap! reports update :warnings vconj warning)
  nil)

(defmethod reporting-callbacks/analyzer-exception TrackingReporter [{:keys [reports]} exception]
  (swap! reports update :errors vconj exception)
  nil)

(defmethod reporting-callbacks/note TrackingReporter [{:keys [reports]} msg]
  (swap! reports update :note vconj msg)
  nil)
