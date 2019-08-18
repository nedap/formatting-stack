(ns formatting-stack.util.ns
  (:require
   [com.gfredericks.how-to-ns :as how-to-ns]
   [nedap.speced.def :as speced]))

(speced/defn formatted= [^string? original-ns-form
                         ^::speced/nilable ^string? clean-ns-form
                         ^map? how-to-ns-opts]
  (if-not clean-ns-form
    true
    (->> [original-ns-form clean-ns-form]
         (map (fn [form]
                (how-to-ns/format-ns-str form how-to-ns-opts)))
         (apply =))))

(speced/defn replaceable-ns-form
  [^string? filename, ^ifn? ns-cleaner, ^map? how-to-ns-opts]
  (let [buffer (slurp filename)
        original-ns-form-str (-> buffer how-to-ns/slurp-ns-from-string)
        original-ns-form (-> original-ns-form-str read-string)
        clean-ns-form (ns-cleaner original-ns-form)]
    (cond
      (not clean-ns-form)
      nil

      (= original-ns-form clean-ns-form)
      nil

      (formatted= original-ns-form-str clean-ns-form how-to-ns-opts)
      nil

      true
      {:buffer               buffer
       :original-ns-form-str original-ns-form-str
       :final-ns-form-str    (how-to-ns/format-ns-str clean-ns-form how-to-ns-opts)})))

(speced/defn replace-ns-form!
  [^string? filename, ^ifn? ns-cleaner, ^string? message, ^map? how-to-ns-opts]
  (when-let [{:keys [final-ns-form-str
                     original-ns-form-str
                     buffer]} (replaceable-ns-form filename ns-cleaner how-to-ns-opts)]
    (println message filename)
    (->> original-ns-form-str
         count
         (subs buffer)
         (str final-ns-form-str)
         (spit filename))))
