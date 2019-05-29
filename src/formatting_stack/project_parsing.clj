(ns formatting-stack.project-parsing
  (:require
   [clojure.java.classpath :as classpath]
   [clojure.java.io :as io]
   [clojure.tools.namespace.file :as file]
   [clojure.tools.namespace.find :as find]
   [clojure.tools.namespace.parse :as parse])
  (:import
   (java.io File)))

(defn find-files [dirs platform]
  (->> dirs
       (map io/file)
       (map #(.getCanonicalFile ^File %))
       (filter #(.exists ^File %))
       (mapcat #(find/find-sources-in-dir % platform))
       (map #(.getCanonicalFile ^File %))))

(defn project-namespaces
  "Returns all the namespaces contained or required in the current project.

  Includes third-party dependencies."
  []
  (->> (find-files (classpath/classpath-directories) find/clj)
       (mapcat (fn [file]
                 (let [decl (-> file file/read-file-ns-decl)
                       n (-> decl parse/name-from-ns-decl)
                       deps (-> decl parse/deps-from-ns-decl)]
                   (conj deps n))))
       (distinct)
       (filter identity)
       (keep find-ns)))
