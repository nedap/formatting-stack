(ns formatting-stack.strategies.impl
  (:require
   [clojure.java.shell :refer [sh]]
   [clojure.spec.alpha :as spec]
   [clojure.string :as string]
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

(speced/def-with-doc ::file-entry
  "Not necessarily a filename,
  e.g. the string \"M  src/formatting_stack/strategies/impl.clj\" is a valid output."
  string?)

(spec/def ::file-entries (spec/coll-of ::file-entry))

(speced/defn ^::file-entries file-entries
  [& args]
  (->> args (apply sh) :out string/split-lines (filter seq)))

(def separator-pattern (re-pattern File/separator))

(def ^:dynamic *skip-existing-files-check?* false)

(spec/def ::existing-files (spec/coll-of (speced/fn [^string? s]
                                           (if *skip-existing-files-check?*
                                             true
                                             (-> s File. .exists)))))

(speced/defn ^::existing-files absolutize [command, ^::file-entries file-entries]
  (let [toplevel-fragments (case command
                             "git" (-> (sh "git" "rev-parse" "--show-toplevel")
                                       (:out)
                                       (string/split #"\n")
                                       (first)
                                       (string/split separator-pattern)))]
    (->> file-entries
         (map (fn [filename]
                (->> (string/split filename separator-pattern)
                     (concat toplevel-fragments)
                     (string/join File/separator)))))))

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
  (if-not (-> filename File. .exists)
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
                                       (-> f File. .exists)))
    true                     (remove #(string/ends-with? % "project.clj"))
    true                     (filter readable?)))

(speced/defn ^boolean? dir-contains?
  [^string? dirname, ^File file]
  (->> (file-seq (File. dirname))
       (map (speced/fn [^File f]
              (-> f .getCanonicalPath)))
       (some #{(-> file .getCanonicalPath)})
       (boolean)))
