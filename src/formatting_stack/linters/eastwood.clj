(ns formatting-stack.linters.eastwood
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [eastwood.lint]
   [formatting-stack.linters.eastwood.impl :as impl]
   [formatting-stack.protocols.linter :as linter]
   [formatting-stack.util :refer [ns-name-from-filename silence]]
   [medley.core :refer [assoc-some deep-merge]]
   [nedap.utils.modular.api :refer [implement]])
  (:import
   (java.io File)))

(def default-eastwood-options
  (-> eastwood.lint/default-opts
      (assoc :rethrow-exceptions? true)))

(defn lint! [{:keys [options]} filenames]
  (let [namespaces (->> filenames
                        (remove #(str/ends-with? % ".edn"))
                        (keep ns-name-from-filename))
        reports    (atom nil)
        exceptions (atom nil)]
    
    (silence
      (try
        (-> options
            (assoc :namespaces namespaces)
            (eastwood.lint/eastwood (impl/->TrackingReporter reports)))
        (catch Exception e
          (swap! exceptions conj e))))
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
         (into (impl/exceptions->reports @exceptions)))))

(defn new [{:keys [eastwood-options]
            :or   {eastwood-options {}}}]
  (implement {:id ::id
              :options (deep-merge default-eastwood-options eastwood-options)}
    linter/--lint! lint!))
