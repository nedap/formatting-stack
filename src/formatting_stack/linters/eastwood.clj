(ns formatting-stack.linters.eastwood
  (:require
   [clojure.string :as str]
   [eastwood.lint]
   [formatting-stack.linters.eastwood.impl :as impl]
   [formatting-stack.protocols.linter :as linter]
   [formatting-stack.util :refer [ns-name-from-filename]]
   [medley.core :refer [assoc-some deep-merge]]
   [nedap.utils.modular.api :refer [implement]])
  (:import
   (java.io File)))

(def default-eastwood-options
  ;; Avoid false positives or undesired checks:
  (let [linters (remove #{:suspicious-test :unused-ret-vals :constant-test :wrong-tag}
                        eastwood.lint/default-linters)]
    (-> eastwood.lint/default-opts
        (assoc :linters linters))))

(defn lint! [{:keys [options]} filenames]
  (let [namespaces (->> filenames
                        (remove #(str/ends-with? % ".edn"))
                        (keep ns-name-from-filename))
        reports    (atom nil)
        output     (with-out-str
                     (binding [*warn-on-reflection* true]
                       (eastwood.lint/eastwood (assoc options :namespaces namespaces)
                                               (impl/->TrackingReporter reports))))]
    (->> @reports
         :warnings
         (map :warn-data)
         (map (fn [{:keys [uri-or-file-name linter] :strs [warning-details-url] :as m}]
                (assoc-some m
                            :level               :warning
                            :source              (keyword "eastwood" (name linter))
                            :warning-details-url warning-details-url
                            :filename            (if (string? uri-or-file-name)
                                                   uri-or-file-name
                                                   (-> ^File uri-or-file-name .getCanonicalPath)))))
         (concat (impl/warnings->reports output)))))

(defn new [{:keys [eastwood-options]
            :or   {eastwood-options {}}}]
  (implement {:id ::id
              :options (deep-merge default-eastwood-options eastwood-options)}
    linter/--lint! lint!))
