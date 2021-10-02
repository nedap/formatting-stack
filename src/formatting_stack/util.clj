(ns formatting-stack.util
  (:require
   [clojure.java.io :as io]
   [clojure.spec.alpha :as spec]
   [clojure.string :as string]
   [clojure.tools.namespace.file :as file]
   [clojure.tools.namespace.parse :as parse]
   [clojure.tools.reader.reader-types :refer [indexing-push-back-reader push-back-reader]]
   [medley.core :refer [find-first]]
   [nedap.speced.def :as speced]
   [nedap.utils.collections.eager :refer [partitioning-pmap]]
   [nedap.utils.collections.seq :refer [distribute-evenly-by]]
   [nedap.utils.spec.predicates :refer [present-string?]])
  (:import
   (clojure.lang IBlockingDeref IPending)
   (java.io File StringWriter)))

(defmacro rcomp
  "Like `comp`, but in reverse order.
  With it, members of `->>` chains can consistently be read left-to-right."
  [& fns]
  (cons `comp (reverse fns)))

(defn dissoc-by [m f]
  (->> m
       (filter (rcomp first f))
       (into {})))

(speced/defn read-ns-decl
  "Reads ns declaration in file with line/column metadata"
  [^string? filename]
  (when-not (-> filename File. .isDirectory)
    (try
      (with-open [reader (-> filename io/reader push-back-reader indexing-push-back-reader)]
        (parse/read-ns-decl reader))
      (catch Exception e
        (if (-> e ex-data :type #{:reader-exception})
          nil
          (throw e))))))

(def ns-name-from-filename (rcomp file/read-file-ns-decl parse/name-from-ns-decl))

(defn serialized-writer [out]
  (let [state (atom {})
        write! (fn [input]
                 (swap! state update (Thread/currentThread) vec)
                 (swap! state update (Thread/currentThread) conj input)
                 nil)]
    (proxy [java.io.Writer] []
      (append [& input]
        (write! input))
      (close [])
      (flush []
        (binding [*out* out
                  *err* *out*]
          (let [contents (-> @state (get (Thread/currentThread)))]
            (->> contents
                 (apply concat)
                 (apply str)
                 (print))))
        (swap! state assoc (Thread/currentThread) [])
        nil)
      (write [& input]
        (write! input)))))

(defmacro with-serialized-output
  {:style/indent 0}
  [& forms]
  `(binding [*out* (serialized-writer *out*)
             *err* *out*
             *flush-on-newline* true]
     ~@forms))

(speced/defn report-processing-error [^Throwable e, filename, ^ifn? f]
  {:level     :exception
   :source    :formatting-stack/report-processing-error
   :filename  filename
   :msg       (str "Encountered an exception while running " (pr-str f))
   :exception e})

(spec/def ::non-lazy-result
  (fn [x]
    (cond
      (sequential? x)              (vector? x)
      (instance? IBlockingDeref x) false
      (instance? IPending x)       false
      true                         true)))

(defn process-in-parallel! [f files]
  (->> files
       (distribute-evenly-by {:f (fn [^String filename]
                                   (-> (File. filename) .length))})
       (partitioning-pmap (bound-fn [filename]
                            (try
                              (let [v (f filename)]
                                (assert (spec/valid? ::non-lazy-result v)
                                        (pr-str "Parallel processing shouldn't return lazy computations"
                                                f))
                                v)
                              (catch Exception e
                                (report-processing-error e filename f))
                              (catch AssertionError e
                                (report-processing-error e filename f)))))))

(defn require-from-ns-decl [ns-decl]
  (->> ns-decl
       (find-first (fn [x]
                     (and (sequential? x)
                          (#{:require} (first x)))))))

;; `require` is not thread-safe in Clojure.
(def require-lock
  (Object.))

(speced/defn try-require [^present-string? filename]
  (try
    (when-let [namespace (some-> filename file/read-file-ns-decl parse/name-from-ns-decl)]
      (locking require-lock
        (require namespace)))
    true
    (catch Exception _
      false)))

(speced/defn ensure-coll [^some? x]
  (if (coll? x)
    x
    [x]))

;; Rationale: https://github.com/nedap/formatting-stack/pull/109/files#r376891779
(speced/defn ensure-sequential [^some? x]
  (if (sequential? x)
    x
    [x]))

(def ansi-colors
  {:reset  "[0m"
   :red    "[031m"
   :green  "[032m"
   :yellow "[033m"
   :cyan   "[036m"
   :grey   "[037m"})

(defn colorize [s color]
  (str \u001b (ansi-colors color) s \u001b (ansi-colors :reset)))

(defn colorize-diff
  "Colorizes a diff-text extracted from #'cljfmt.diff"
  [diff-text] ;; see https://git.io/Jkoqb
  (-> diff-text
      (string/replace #"(?m)^(@@.*@@)$" (colorize "$1" :cyan))
      (string/replace #"(?m)^(\+(?!\+\+).*)$" (colorize "$1" :green))
      (string/replace #"(?m)^(-(?!--).*)$" (colorize "$1" :red))))

(defmacro silence
  "Execute body without printing to `*out*` or `*err*`"
  {:style/indent 0}
  [& body]
  `(let [s# (StringWriter.)]
     (binding [*out* s#
               *err* s#]
       ~@body)))

;; see https://stackoverflow.com/a/23221442/2046200
(defn partition-between
  "Applies f to each value and the next value, splitting each time f returns a new value.
  Returns a lazy sequence or partitions."
  [pred coll]
  (let [switch (reductions not= true (map pred coll (rest coll)))]
    (map (partial map first) (partition-by second (map list coll switch)))))

(speced/defn resolve-sym [^qualified-symbol? sym]
  (deref (requiring-resolve sym)))

(speced/defn resolve-keyword [^keyword? kwd]
  (try
    (require (str (namespace kwd) "." (name kwd)))
    true
    (catch Exception _
      false)))

(defn accumulate [m k v]
  (update m k (fnil conj []) v))
