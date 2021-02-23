(ns formatting-stack.linters.line-length
  (:require
   [clojure.string :as string]
   [formatting-stack.protocols.linter :as linter]
   [formatting-stack.util :refer [ensure-sequential process-in-parallel!]]
   [nedap.utils.modular.api :refer [implement]]))

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
                           :msg      (str "Line exceeding " max-line-length " columns.")}))))
       (filter some?)
       ;; remove duplicates within `merge-threshold` lines
       (reduce (fn [memo {:keys [line] :as report}]
                 (let [diff (- (or line 0)
                               (or (:line (last memo)) 0))]
                   (if (< diff merge-threshold)
                     (conj (vec (butlast memo)) report)
                     (conj memo report))))
               [])))

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
