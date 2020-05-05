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
   (io.reflectoring.diffparser.api UnifiedDiffParser)))

(defn unified-diff
  [filename original revised]
  (->> (DiffUtils/generateUnifiedDiff
        (->> filename (str "a"))
        (->> filename (str "b"))
        (string/split-lines original)
        (DiffUtils/diff (string/split-lines original) (string/split-lines revised))
        3)
       (string/join "\n")))

(spec/def ::begin pos-int?)
(spec/def ::end pos-int?)
(spec/def ::filename present-string?)

(spec/def ::line-numbers
  (spec/coll-of (spec/keys :req-un [::begin ::end ::filename])))

(speced/defn ^::line-numbers diff->line-numbers
  "Returns maps of consecutive removals/changes (as `:begin` to `:end`) per `:filename` in `diff`."
  [^string? diff]
  (->> (io/input-stream (.getBytes diff))
       (.parse (UnifiedDiffParser.))
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
                                               :begin    lineNumber
                                               :end      lineNumber}))))
                              []))))))
