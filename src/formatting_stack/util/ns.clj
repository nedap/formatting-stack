(ns formatting-stack.util.ns
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.string :as string]
   [clojure.tools.reader :as tools.reader]
   [com.gfredericks.how-to-ns :as how-to-ns]
   [formatting-stack.util :refer [rcomp]]
   [nedap.speced.def :as speced]
   [nedap.utils.spec.api :refer [check!]]))

(spec/def ::ns-form (spec/and sequential?
                              (rcomp first #{'ns `ns})))

(spec/def ::ns-form-str (spec/and string?
                                  (complement string/blank?)))

(spec/def ::clean-ns-form (spec/nilable string?))

(speced/defn formatted= [^string? original-ns-form
                         ^::clean-ns-form clean-ns-form
                         ^map? how-to-ns-opts]
  (if-not clean-ns-form
    true
    (->> [original-ns-form clean-ns-form]
         (map (fn [form]
                (how-to-ns/format-ns-str form how-to-ns-opts)))
         (apply =))))

(speced/defn safely-read-ns-form [^::ns-form-str ns-form-str]
  (-> (tools.reader/read-string {:read-cond :preserve
                                 :features  #{:clj :cljs}}
                                (str "[ " ns-form-str " ]"))
      first))

(speced/defn replaceable-ns-form
  [^string? filename, ^ifn? ns-cleaner, ^map? how-to-ns-opts]
  (let [buffer (slurp filename)
        original-ns-form-str (-> buffer how-to-ns/slurp-ns-from-string)
        original-ns-form (-> original-ns-form-str safely-read-ns-form)
        clean-ns-form (ns-cleaner original-ns-form)]
    (check! ::ns-form-str   original-ns-form-str
            ::ns-form       original-ns-form
            ::clean-ns-form clean-ns-form)
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
