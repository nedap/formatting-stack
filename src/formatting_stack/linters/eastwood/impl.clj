(ns formatting-stack.linters.eastwood.impl
  (:require
   [clojure.string :as string]
   [eastwood.reporting-callbacks :as reporting-callbacks]
   [formatting-stack.protocols.spec :as protocols.spec]
   [nedap.speced.def :as speced]))

(speced/defn ^boolean? wrong-pre-post-false-positives
  "Removes false positives for dynamic vars https://git.io/fhQTx"
  [{{{[_fn* [_arglist [_assert v]]] :form} :ast} :wrong-pre-post}]
  (let [varname (-> v str (string/split #"\/") last)]
    (= \*
       (first varname)
       (last varname))))

(speced/defn ^::protocols.spec/reports warnings->reports
  [^string? warnings]
  (->> warnings
       (re-seq #"(.*):(\d+):(\d+): Reflection warning - (.+)\.")
       (map (fn [[_, filename, line, column, msg]]
              {:filename filename
               :line     (Long/parseLong line)
               :column   (Long/parseLong column)
               :msg      msg
               :level    :warning
               :source   :eastwood/warn-on-reflection}))))

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
