(ns formatting-stack.kondo-classpath-cache
  "Holds a cache for the entire classpath, to make better use of the project-wide analysis capabilities.

  Only needs to be created once, as the classpath never changes."
  (:require
   [clj-kondo.core :as kondo]
   [clojure.string :as string])
  (:import
   (java.io File)))

(def cache-dir ".clj-kondo")

(def classpath-cache
  (delay
    (let [files (-> (System/getProperty "java.class.path")
                    (string/split #"\:"))]
      (-> ".clj-kondo" File. .mkdirs)
      (kondo/run! {:lint      files
                   :cache-dir cache-dir}))))
