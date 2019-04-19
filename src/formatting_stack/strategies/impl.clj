(ns formatting-stack.strategies.impl
  (:require
   [clojure.java.shell :refer [sh]]
   [clojure.string :as str]
   [clojure.tools.namespace.parse :as parse]
   [clojure.tools.reader]
   [clojure.tools.reader.reader-types :refer [push-back-reader]])
  (:import
   (java.io File)))

;; modified, added, renamed
(def git-completely-staged-regex #"^(M|A|R)  ")

;; modified, added
(def git-not-completely-staged-regex #"^( M|AM|MM|AD| D|\?\?|) ")

(defn file-entries [& args]
  (->> args (apply sh) :out str/split-lines (filter seq)))

(def ^:dynamic *filter-existing-files?* true)

(defn unreadable?
  "Is this file unreadable to clojure.tools.reader? (because of custom reader tags, unbalanced parentheses or such)"
  [^String filename]
  (if-not (-> filename File. .exists)
    false ;; undecidable
    (let [contents (-> filename slurp)
          wrapped (str "[" contents "]")]
      (try
        (-> wrapped push-back-reader clojure.tools.reader/read nil?)
        (catch Exception e
          true)))))

(defn extract-clj-files [files]
  (cond->> files
    true                     (filter #(re-find #"\.(clj|cljc|cljs|edn)$" %))
    *filter-existing-files?* (filter (fn [^String f]
                                       (-> f File. .exists)))
    true                     (remove #(str/ends-with? % "project.clj"))
    true                     (remove unreadable?)))
