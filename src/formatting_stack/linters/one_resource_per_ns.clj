(ns formatting-stack.linters.one-resource-per-ns
  "This linter ensures that there's exactly once classpath resource per namespace and extension.

  Note that generally it's fine to define identically-named Clojure/Script namespaces with _different_ extensions,
  so that is allowed."
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.string :as string]
   [clojure.tools.namespace.file :as file]
   [formatting-stack.protocols.linter :as linter]
   [formatting-stack.util :refer [process-in-parallel!]]
   [formatting-stack.util.ns :as util.ns]
   [nedap.speced.def :as speced]
   [nedap.utils.modular.api :refer [implement]]
   [nedap.utils.spec.predicates :refer [present-string?]]))

(spec/def ::resource-path (spec/and present-string?
                                    (complement #{\. \! \? \-})
                                    (fn [x]
                                      (re-find #"\.clj([cs])?$" x))))

(speced/defn ^::resource-path ns-decl->resource-path [^::util.ns/ns-form ns-decl, extension]
  (-> ns-decl
      second
      str
      munge
      (string/replace "." "/")
      (str extension)))

(speced/defn resource-path->filenames [^::resource-path resource-path]
  (->> (-> (Thread/currentThread)
           (.getContextClassLoader)
           (.getResources resource-path))
       (enumeration-seq)
       (distinct) ;; just in case
       (mapv str)))

(speced/defn analyze [^present-string? filename]
  (for [extension [".clj" ".cljs" ".cljc"]
        :let [decl (-> filename file/read-file-ns-decl)
              resource-path (ns-decl->resource-path decl extension)
              filenames (resource-path->filenames resource-path)]
        :when (-> filenames count (> 1))]
    {:extension extension
     :ns-name   (-> decl second)
     :filenames filenames}))

(defn lint! [this filenames]
  (->> filenames
       (process-in-parallel! (fn [filename]
                               (->> filename
                                    analyze
                                    (run! (speced/fn [{:keys [^symbol? ns-name, ^coll? filenames]}]
                                            (println "Warning: the namespace"
                                                     (str "`" ns-name "`")
                                                     "is defined over more than one file.\nFound:"
                                                     (->> filenames (interpose ", ") (apply str))))))))))

(speced/defn new [^map? opts]
  (implement opts
    linter/--lint! lint!))
