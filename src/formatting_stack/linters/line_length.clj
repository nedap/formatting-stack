(ns formatting-stack.linters.line-length
 (:require
  [clojure.string :as string]
  [formatting-stack.protocols.linter :as linter]
  [formatting-stack.util :refer [process-in-parallel!]]
  [nedap.utils.modular.api :refer [implement]]))

(defn exceeding-lines [threshold filename]
  (->> (-> filename slurp (string/split #"\n"))
       (map-indexed (fn [i row]
                      (when (< threshold (count row))
                        {:filename filename
                         :linter   :formatting-stack/line-length
                         :level    :warning
                         :column   (+ 1 threshold)
                         :line     (+ 1 i)
                         :msg      (str "Line exceeding " threshold " columns")})))
       (remove nil?)))

(defn lint! [{:keys [max-line-length]} filenames]
  (->> filenames
       (process-in-parallel! (partial exceeding-lines max-line-length))
       (mapcat identity)))

(defn new [{:keys [max-line-length]
            :or {max-line-length 130}}]
  (implement {:max-line-length max-line-length}
             linter/--lint! lint!))
