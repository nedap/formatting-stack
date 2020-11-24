(ns formatting-stack.util.diff
  (:require
   [clojure.java.data :as data]
   [clojure.java.io :as io]
   [clojure.spec.alpha :as spec]
   [clojure.string :as string]
   [formatting-stack.util :refer [rcomp]]
   [nedap.speced.def :as speced]
   [nedap.utils.spec.predicates :refer [present-string?]])
  (:import
   (difflib DiffUtils)
   (io.reflectoring.diffparser.api UnifiedDiffParser)
   (java.io File)
   (java.util.regex Pattern)))

(spec/def ::start pos-int?)
(spec/def ::end pos-int?)
(spec/def ::filename present-string?)

(speced/def-with-doc ::line-numbers
  "maps of consecutive removals/changes (as `:start` to `:end`) per `:filename`."
  (spec/coll-of (spec/keys :req-un [::start ::end ::filename])))

(speced/defn ^::line-numbers diff->line-numbers
  [^string? diff]
  (->> (.parse (UnifiedDiffParser.) (io/input-stream (.getBytes diff)))
       (data/from-java)
       (mapcat (speced/fn [{:keys [^present-string? toFileName ^coll? hunks]}]
                 (->> hunks
                      (mapcat (speced/fn [{:keys [^coll? lines] {:keys [^pos-int? lineStart]} :fromFileRange}]
                                (->> lines
                                     (remove (rcomp :lineType #{"TO"}))
                                     (map-indexed (fn [idx line] (assoc line :lineNumber (+ idx lineStart)))))))
                      (reduce (speced/fn [ret {:keys [^{::speced/spec #{"FROM" "NEUTRAL"}} lineType
                                                      ^pos-int? lineNumber]}]
                                (let [{:keys [end] :as current} (last ret)]
                                  (cond
                                    (#{"NEUTRAL"} lineType)
                                    ret

                                    (= end (dec lineNumber))
                                    (assoc ret (dec (count ret)) (assoc current :end lineNumber)) ;; update last map

                                    :else
                                    (conj ret {:filename (string/replace-first toFileName "b/" "")
                                               :start lineNumber
                                               :end   lineNumber}))))
                              []))))))

(def diff-context-size 3)

(def ^:dynamic *to-absolute-path-fn*
  (fn [filename]
    (->> (string/split filename (re-pattern (Pattern/quote File/separator)))
         ^File (apply io/file)
         .getCanonicalPath)))

(speced/defn ^string? unified-diff
  "derives a patch derived from the original and revised file contents in a Unified Diff format"
  ([^string? filename ^string? original ^string? revised]
   (letfn [(lines [s]
             (string/split s #"\n"))
           (unlines [ss]
             (string/join "\n" ss))]
     (unlines (DiffUtils/generateUnifiedDiff
               (->> filename *to-absolute-path-fn* (str "a"))
               (->> filename *to-absolute-path-fn* (str "b"))
               (lines original)
               (DiffUtils/diff (lines original) (lines revised))
               diff-context-size)))))
