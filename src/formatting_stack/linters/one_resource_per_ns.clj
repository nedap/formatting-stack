(ns formatting-stack.linters.one-resource-per-ns
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.string :as string]
   [clojure.tools.namespace.file :as file]
   [formatting-stack.protocols.linter :as linter]
   [formatting-stack.util :refer [process-in-parallel!]]
   [nedap.speced.def :as speced]
   [nedap.utils.modular.api :refer [implement]]
   [nedap.utils.spec.predicates :refer [present-string?]]))

(spec/def ::ns-decl (spec/and sequential?
                              (fn [x]
                                (->> x first #{'ns `ns}))))

(spec/def ::resource-path (spec/and present-string?
                                    (complement #{\. \! \? \-})
                                    (fn [x]
                                      (re-find #"\.clj([cs])?$" x))))

(speced/defn ^::resource-path ns-decl->resource-path [^::ns-decl ns-decl, extension]
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
     :decl      decl
     :filenames filenames}))

(defn lint! [this filenames]
  (->> filenames
       (process-in-parallel! (fn [filename]
                               (->> filename
                                    analyze
                                    (run! (speced/fn [{:keys [^some? decl, ^some? filenames]}]
                                            (println "Warning: the namespace"
                                                     (str "`" (-> decl second) "`")
                                                     "is defined over more than one file.\nFound:"
                                                     (->> filenames (interpose ", ") (apply str))))))))))

(speced/defn new [^map? opts]
  (implement opts
    linter/--lint! lint!))
