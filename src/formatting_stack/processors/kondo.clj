(ns formatting-stack.processors.kondo
  "Creates cache for entire classpath to make better use of the project-wide analysis capabilities."
  (:require
   [clj-kondo.core :as clj-kondo]
   [clojure.string :as str]
   [formatting-stack.protocols.processor :as processor]
   [nedap.utils.modular.api :refer [implement]])
  (:import (java.io File)))

(defn process! [_ _]
  (let [files (-> (System/getProperty "java.class.path")
                  (str/split #"\:"))
        cache-dir ".clj-kondo"]
    (-> ".clj-kondo" File. .mkdirs)
    (clj-kondo/run! {:lint files
                     :cache-dir cache-dir}))
  nil)

(defn new []
  (implement {}
    processor/--process! process!))
