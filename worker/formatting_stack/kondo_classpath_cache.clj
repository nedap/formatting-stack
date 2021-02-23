(ns formatting-stack.kondo-classpath-cache
  "Holds a cache for the entire classpath, to make better use of the project-wide analysis capabilities.

  Only needs to be created once, as the classpath never changes."
  (:require
   [clj-kondo.core :as kondo]
   [clojure.string :as string]))

(def classpath-cache
  (future
    (let [files (-> (System/getProperty "java.class.path")
                    (string/split #"\:"))]
      (->> (partition-all 100 files) ;; prevent OOM when classpath is large
           (mapv #(kondo/run! {:lint      %
                               :parallel  true
                               :cache     true}))))))
