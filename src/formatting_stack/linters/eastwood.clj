(ns formatting-stack.linters.eastwood
  (:require
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [eastwood.lint]
   [formatting-stack.linters.eastwood.impl :as impl]
   [formatting-stack.protocols.linter :as linter]
   [formatting-stack.protocols.spec :as protocols.spec]
   [formatting-stack.util :refer [ns-name-from-filename]]
   [medley.core :refer [assoc-some deep-merge]]
   [nedap.speced.def :as speced]
   [nedap.utils.modular.api :refer [implement]]
   [nedap.utils.spec.api :refer [check!]])
  (:import
   (java.io File)))

(def default-eastwood-options
  ;; Avoid false positives or undesired checks:
  (let [linters (remove #{:suspicious-test :unused-ret-vals :constant-test :wrong-tag}
                        eastwood.lint/default-linters)]
    (-> eastwood.lint/default-opts
        (assoc :linters             linters
               :rethrow-exceptions? true))))

(def parallelize-linters? (System/getProperty "formatting-stack.eastwood.parallelize-linters"))

(def config-filename "formatting_stack.clj")

(assert (io/resource (str (io/file "eastwood" "config" config-filename)))
        "The formatting-stack config file must exist and be prefixed by `eastwood/config`
(note that this prefix must not be passed to Eastwood itself).")

(defn lint! [{:keys [options]} filenames]
  {:post [(do
            (assert (check! (speced/fn [^::protocols.spec/reports xs]
                              (let [output (->> xs (keep :filename) (set))]
                                (set/subset? output (set filenames))))
                            %)
                    "The `:filename`s returned from Eastwood should be a subset of this function's `filenames`.
Otherwise, it would mean that our filename absolutization out of Eastwood reports is buggy.")
            true)]}
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
         (map (fn [{:keys [uri-or-file-name linter] :strs [warning-details-url] :as m}]
                (assoc-some m
                            :level               :warning
                            :source              (keyword "eastwood" (name linter))
                            :warning-details-url warning-details-url
                            :filename            (speced/let [^::speced/nilable ^String s (when (string? uri-or-file-name)
                                                                                            uri-or-file-name)
                                                              ^File file (or (some-> s File.)
                                                                             uri-or-file-name)]
                                                   (-> file .getCanonicalPath)))))
         (concat (impl/warnings->reports output)
                 (impl/exceptions->reports @exceptions)))))

(defn new [{:keys [eastwood-options]
            :or   {eastwood-options {}}}]
  (implement {:id ::id
              :options (deep-merge default-eastwood-options eastwood-options)}
    linter/--lint! lint!))
