(ns formatting-stack.project-parsing
  (:require
   [clojure.java.classpath :as classpath]
   [clojure.java.io :as io]
   [clojure.tools.namespace.find :as find]
   [clojure.tools.namespace.parse :as parse]
   [formatting-stack.formatters.clean-ns.impl :refer [ns-form-of]]
   [nedap.speced.def :as speced]
   [nedap.utils.collections.eager :refer [partitioning-pmap]])
  (:import
   (java.io File)))

(defn find-files [dirs platform]
  (->> dirs
       (map io/file)
       (map (speced/fn [^File f]
              (-> f .getCanonicalFile)))
       (filter (speced/fn [^File f]
                 (-> f .exists)))
       (mapcat (speced/fn [^File f]
                 (-> f (find/find-sources-in-dir platform))))
       (map (speced/fn [^File f]
              (-> f .getCanonicalFile)))))

(defn project-namespaces
  "Returns all the namespaces contained or required in the current project.

  Includes third-party dependencies."
  []
  (->> (find-files (classpath/classpath-directories) find/clj)

       (partitioning-pmap (speced/fn [^File file]
                            (let [decl (-> file str ns-form-of)
                                  n (some-> decl parse/name-from-ns-decl)
                                  deps (some-> decl parse/deps-from-ns-decl)]
                              (some-> deps (conj n)))))
       (apply concat)
       (distinct)
       (filter identity)
       (keep find-ns)))
