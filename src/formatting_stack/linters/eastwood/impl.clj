(ns formatting-stack.linters.eastwood.impl
  (:require
   [clojure.string :as string]
   [eastwood.reporting-callbacks :as reporting-callbacks]
   [formatting-stack.protocols.spec :as protocols.spec]
   [nedap.speced.def :as speced]))

(speced/defn ^boolean? contains-dynamic-assertions?
  "Does this linting result refer to a :pre/:post containing dynamic assertions?

See https://git.io/fhQTx"
  [{{{[_fn* [_arglist & fn-tails]] :form} :ast} :wrong-pre-post
    msg                                         :msg
    :as                                         x}]
  (->> fn-tails
       (some (fn [tail]
               (when (and (coll? tail)
                          (= 'clojure.core/assert
                             (first tail)))
                 (let [v (second tail)
                       v-name (when (symbol? v)
                                (name v))]
                   (and v-name
                        (string/includes? msg v-name) ;; make sure it's the same symbol as in msg
                        (= \*
                           (first v-name)
                           (last v-name)))))))
       (boolean)))

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
