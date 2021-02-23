(ns formatting-stack.linters.eastwood
  (:require
   [clojure.java.io :as io]
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
  (assoc eastwood.lint/default-opts
         ;; Avoid false positives or undesired checks:
         :exclude-linters     #{:suspicious-test :unused-ret-vals :constant-test :wrong-tag}
         :parallel?           :naive
         :rethrow-exceptions? true))

(def parallelize-linters? (System/getProperty "formatting-stack.eastwood.parallelize-linters"))

(def config-filename "formatting_stack.clj")

(assert (io/resource (str (io/file "eastwood" "config" config-filename)))
        "The formatting-stack config file must exist and be prefixed by `eastwood/config`
(note that this prefix must not be passed to Eastwood itself).")

(defn lint! [{:keys [options]} filenames]
  (let [namespaces (->> filenames
                        (remove #(str/ends-with? % ".edn"))
                        (keep ns-name-from-filename))
        reports    (atom nil)
        exceptions (atom nil)
        output     (with-out-str
                     (binding [*warn-on-reflection* true]
                       (try
                         (cond-> options
                           true                 (assoc :namespaces namespaces)
                           parallelize-linters? (update :builtin-config-files conj config-filename)
                           true                 (eastwood.lint/eastwood (impl/->TrackingReporter reports)))
                         (catch Exception e
                           (swap! exceptions conj e)))))]
    (->> @reports
         :warnings
         (map :warn-data)
         (map (fn [{:keys [uri-or-file-name msg line column linter] :strs [warning-details-url]}]
                (assoc-some {:level         :warning
                             :source        (keyword "eastwood" (name linter))
                             :filename      (if (string? uri-or-file-name)
                                              uri-or-file-name
                                              (-> ^File uri-or-file-name .getCanonicalPath))
                             :column        column
                             :msg           msg
                             :line          line}
                            :warning-details-url warning-details-url)))
         (concat (impl/warnings->reports output)
                 (impl/exceptions->reports @exceptions)))))

(defn new [{:keys [eastwood-options]
            :or   {eastwood-options {}}}]
  (implement {:id ::id
              :options (deep-merge default-eastwood-options eastwood-options)}
    linter/--lint! lint!))
