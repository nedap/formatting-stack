(ns formatting-stack.strategies.impl
  (:require
   [clojure.java.shell :refer [sh]]
   [clojure.string :as str]
   [clojure.tools.namespace.file :as file]
   [clojure.tools.namespace.parse :as parse]
   [formatting-stack.formatters.clean-ns.impl :refer [safely-read-ns-contents]]
   [formatting-stack.util :refer [read-ns-decl]]
   [nedap.speced.def :as speced])
  (:import
   (clojure.lang Namespace)
   (java.io File)))

;; modified, added, renamed
(def git-completely-staged-regex #"^(M|A|R)  ")

;; modified, added
(def git-not-completely-staged-regex #"^( M|AM|MM|AD| D|\?\?|) ")

(defn file-entries [& args]
  (->> args (apply sh) :out str/split-lines (filter seq)))

(def ^:dynamic *filter-existing-files?* true)

(defn safe-the-ns [ns-name]
  (try
    (the-ns ns-name)
    (catch Exception _)))

(speced/defn ^::speced/nilable ^Namespace filename->ns [^string? filename]
  (some-> filename read-ns-decl parse/name-from-ns-decl safe-the-ns))

(defn readable?
  "Is this file readable to clojure.tools.reader? (given custom reader tags, unbalanced parentheses or such)"
  [^String filename]
  (if-not (-> filename clojure.java.io/file .exists)
    true ;; undecidable
    (try
      (let [ns-obj (filename->ns filename)]
        (and (do
               (if-not ns-obj
                 true
                 (-> filename slurp (safely-read-ns-contents ns-obj)))
               true)
             (if-let [decl (-> filename file/read-file-ns-decl)]
               (do
                 (-> decl parse/deps-from-ns-decl) ;; no exceptions thrown
                 true)
               true)))
      (catch Exception _
        false)
      (catch AssertionError _
        false))))

(defn extract-clj-files [files]
  (cond->> files
    true                     (filter #(re-find #"\.(clj|cljc|cljs|edn)$" %))
    *filter-existing-files?* (filter (fn [^String f]
                                       (-> f clojure.java.io/file .exists)))
    true                     (remove #(str/ends-with? % "project.clj"))
    true                     (filter readable?)))

(speced/defn ^boolean? dir-contains?
  [^string? dirname, ^File file]
  (->> (file-seq (clojure.java.io/file dirname))
       (map (speced/fn [^File f]
              (-> f .getCanonicalPath)))
       (some #{(-> file .getCanonicalPath)})
       (boolean)))
