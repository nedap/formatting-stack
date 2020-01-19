(ns formatting-stack.processors.kondo
  "Creates cache for entire classpath to make better use of the project-wide analysis capabilities."
  (:require
   [clj-kondo.core :as clj-kondo]
   [clojure.string :as str]
   [formatting-stack.protocols.processor :as processor]
   [nedap.utils.modular.api :refer [implement]])
  (:import (java.io File)))

(defn process! [{:keys [initialized?]} files]
  (let [cache-dir ".clj-kondo"]
    (-> cache-dir File. .mkdirs)
    (when-not @initialized?
      (reset! initialized? true)
      (clj-kondo/run! {:lint (-> (System/getProperty "java.class.path")
                                 (str/split #"\:"))
                       :cache-dir cache-dir}))
    (clj-kondo/run! {:lint files
                     :cache-dir cache-dir}))
  nil)

(defn new []
  (implement {:initialized? (atom false)}
    processor/--process! process!))
