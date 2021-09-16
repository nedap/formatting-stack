(ns formatting-stack.kondo-classpath-cache
  "Holds a cache for the entire classpath, to make better use of the project-wide analysis capabilities.

  Only needs to be created once, as the classpath never changes."
  (:require
   [clj-kondo.core :as kondo]
   [clojure.string :as string])
  (:import
   (java.io File)))

(def runner-mapping
  {"future" `future
   "delay"  `delay})

(def ^:private runner-choice
  (-> runner-mapping
      (get (System/getProperty "formatting-stack.kondo-classpath-cache.runner", "future"))
      (doto assert)))

(defmacro runner
  {:style/indent 0}
  [& body]
  {:pre [(seq body)]}
  (apply list runner-choice body))

;; Use kondo's official default config dir, so that we don't bloat consumers' project layouts:
(def ^String cache-parent-dir ".clj-kondo")

;; Don't use .clj-kondo directly since it can be accessed concurrently (e.g. f-s + a second Kondo from VS Code):
(def ^String cache-subdir "formatting-stack-cache")

(def cache-dir (str cache-parent-dir File/separator cache-subdir))

(def classpath-cache
  (runner
    (let [files (-> (System/getProperty "java.class.path")
                    (string/split #"\:"))]
      (-> (File. cache-parent-dir cache-subdir) .mkdirs)
      (kondo/run! {:lint      files
                   :cache-dir cache-dir}))))
