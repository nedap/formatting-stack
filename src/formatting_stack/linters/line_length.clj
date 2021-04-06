(ns formatting-stack.linters.line-length
  (:require
   [clojure.string :as string]
   [formatting-stack.protocols.linter :as linter]
   [formatting-stack.util :refer [ensure-sequential partition-between process-in-parallel!]]
   [nedap.utils.modular.api :refer [implement]]
   [medley.core :refer [assoc-some]]))

(defn exceeding-lines [{:keys [max-line-length merge-threshold]} filename]
  (->> (-> filename slurp string/split-lines)
       (map count)
       (keep-indexed (fn [i column]
                       (when (< max-line-length column)
                         {:column column
                          :line   (inc i)})))
       (partition-between (fn [{first-line :line} {second-line :line}]
                            (< merge-threshold
                               (- second-line first-line))))
       (mapv (fn [reports]
               (let [{first-line :line
                      first-column :column} (first reports)
                     {last-line :line}      (last reports)]
                 {:filename filename
                  :source   :formatting-stack/line-length
                  :level    :warning
                  :line     first-line
                  :column   first-column
                  :msg      (str "Line exceeding " max-line-length " columns"
                                 (when (not= last-line first-line)
                                   (str " (spanning " (inc (- last-line first-line)) " lines)"))
                                 ".")})))))

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
