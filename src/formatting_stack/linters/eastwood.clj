(ns formatting-stack.linters.eastwood
  (:require
   [clojure.string :as str]
   [eastwood.lint]
   [eastwood.util]
   [formatting-stack.linters.eastwood.impl :as impl]
   [formatting-stack.protocols.linter :as linter]
   [formatting-stack.util :refer [ns-name-from-filename]]
   [medley.core :refer [deep-merge]]
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
  (reset! eastwood.util/warning-enable-config-atom []) ;; https://github.com/jonase/eastwood/issues/317
  (let [namespaces (->> filenames
                        (remove #(str/ends-with? % ".edn"))
                        (keep ns-name-from-filename))
        root-dir   (-> (File. "") .getAbsolutePath)
        reports    (atom nil)
        output     (with-out-str
                     (binding [*warn-on-reflection* true]
                       (eastwood.lint/eastwood (assoc options :namespaces namespaces)
                                               (impl/->TrackingReporter reports))))]
    (->> @reports
         :warnings
         (map :warn-data)
         (remove (fn [{{{[_fn* [_arglist [_assert v]]] :form} :ast} :wrong-pre-post}]
                   (= \* ;; False positives for dynamic vars https://git.io/fhQTx
                      (-> v str first)
                      (-> v str last))))
         (map (fn [{:keys [uri-or-file-name linter] :as m}]
                (assoc m
                       :level    :warning
                       :source   (keyword "eastwood" (name linter))
                       :filename (if (string? uri-or-file-name)
                                   uri-or-file-name
                                   (str/replace (-> ^File uri-or-file-name .getPath)
                                                root-dir
                                                "")))))
         (concat (impl/warnings->reports output)))))

(defn new [{:keys [eastwood-options]
            :or   {eastwood-options {}}}]
  (implement {:options (deep-merge default-eastwood-options eastwood-options)}
    linter/--lint! lint!))
