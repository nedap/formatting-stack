(ns formatting-stack.linters.eastwood
  (:require
   [clojure.string :as str]
   [eastwood.lint]
   [eastwood.util]
   [formatting-stack.protocols.linter :as linter]
   [formatting-stack.util :refer [ns-name-from-filename]]
   [medley.core :refer [deep-merge]]
   [nedap.utils.modular.api :refer [implement]])
  (:import (java.io File)))

(def default-eastwood-options
  ;; Avoid false positives or more-annoying-than-useful checks:
  (let [linters (remove #{:suspicious-test :unused-ret-vals :constant-test :wrong-tag}
                        eastwood.lint/default-linters)]
    (-> eastwood.lint/default-opts
        (assoc :linters linters))))

(defrecord TrackingReporter [reports])

(defmethod eastwood.reporting-callbacks/lint-warning TrackingReporter [{:keys [reports]} warning]
  (swap! reports update :warnings (fnil conj []) warning)
  nil)

(defmethod eastwood.reporting-callbacks/analyzer-exception TrackingReporter [{:keys [reports]} exception]
  (swap! reports update :errors (fnil conj []) exception)
  nil)

(defmethod eastwood.reporting-callbacks/note TrackingReporter [{:keys [reports]} msg]
  (swap! reports update :note (fnil conj []) msg)
  nil)

(defn lint! [{:keys [options]} filenames]
  (reset! eastwood.util/warning-enable-config-atom []) ;; https://github.com/jonase/eastwood/issues/317
  (let [namespaces (->> filenames
                        (remove #(str/ends-with? % ".edn"))
                        (keep ns-name-from-filename))
        root-dir   (-> (File. "") .getAbsolutePath)
        reports    (atom nil)]
    (with-out-str
      (eastwood.lint/eastwood (assoc options :namespaces namespaces)
                              (->TrackingReporter reports)))
    (->> (:warnings @reports)
         (map :warn-data)
         (map (fn [{:keys [uri-or-file-name] :as m}]
                (-> m
                  (update :linter (fn [k] (keyword "eastwood" (name k))))
                  (assoc :level :warning
                         :filename (if (string? uri-or-file-name)
                                     uri-or-file-name
                                     (str/replace (-> uri-or-file-name .getPath)
                                                  root-dir
                                                  "")))))))))

(defn new [{:keys [eastwood-options]
            :or {eastwood-options {}}}]
  (implement {:options (deep-merge default-eastwood-options eastwood-options)}
    linter/--lint! lint!))
