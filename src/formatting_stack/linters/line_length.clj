(ns formatting-stack.linters.line-length
  (:require
   [clojure.string :as string]
   [formatting-stack.protocols.linter :as linter]
   [formatting-stack.util :refer [ensure-sequential process-in-parallel!]]
   [nedap.utils.modular.api :refer [implement]]
   [medley.core :refer [assoc-some]]))

(defn exceeding-lines [{:keys [max-line-length merge-threshold]} filename]
  (->> (-> filename slurp string/split-lines)
       (map-indexed (fn [i row]
                      (let [column-count (count row)]
                        (when (< max-line-length column-count)
                          {:filename filename
                           :source   :formatting-stack/line-length
                           :level    :warning
                           :column   column-count
                           :line     (inc i)
                           :line-start (inc i)}))))
       (filter some?)
       ;; remove duplicates within `merge-threshold` lines
       (reduce (fn [memo {:keys [line] :as report}]
                 (let [last-report (last memo)
                       diff (- (or line 0)
                               (or (:line last-report) 0))]
                   (if (< diff merge-threshold)
                     (conj (vec (butlast memo))
                           ;; track 'original' line-start
                           (assoc-some report :line-start (:line-start last-report)))
                     (conj memo report))))
               [])
       (mapv (fn [{:keys [line line-start] :as report}]
               (assoc report
                      :msg (str "Line exceeding " max-line-length " columns"
                                (when (not= line line-start)
                                  (str " (spanning " (- line line-start) " lines)"))
                                ".")
              ;; make sure reporting is on the first line (not the last)
                      :line line-start)))))

(defn lint! [options filenames]
  (->> filenames
       (process-in-parallel! (partial exceeding-lines options))
       (mapcat ensure-sequential)))

(defn new [{:keys [max-line-length merge-threshold]
            :or   {max-line-length 130
                   merge-threshold 10}}]
  (implement {:id ::id
              :merge-threshold merge-threshold
              :max-line-length max-line-length}
    linter/--lint! lint!))
