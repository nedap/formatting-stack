(ns formatting-stack.linters.line-length
  (:require
   [clojure.string :as string]
   [formatting-stack.protocols.linter :as linter]
   [formatting-stack.util :refer [ensure-sequential process-in-parallel!]]
   [nedap.utils.modular.api :refer [implement]]))

(defn exceeding-lines [threshold filename]
  (->> (-> filename slurp (string/split #"\n"))
       (map-indexed (fn [i row]
                      (let [column-count (count row)]
                        (when (< threshold column-count)
                          {:filename filename
                           :source   :formatting-stack/line-length
                           :level    :warning
                           :column   column-count
                           :line     (inc i)
                           :msg      (str "Line exceeding " threshold " columns.")}))))
       (filterv some?)))

(defn lint! [{:keys [max-line-length]} filenames]
  (->> filenames
       (process-in-parallel! (partial exceeding-lines max-line-length))
       (mapcat ensure-sequential)))

(defn new [{:keys [max-line-length]
            :or   {max-line-length 130}}]
  (implement {:id ::id
              :max-line-length max-line-length}
    linter/--lint! lint!))
