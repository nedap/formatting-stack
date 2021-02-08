(ns formatting-stack.formatters.trivial-ns-duplicates
  "This formatter removes 'trivial' duplicate libspecs from the `ns` form.

  Compensates for some `refactor-nrepl.clean-ns` intricacies, and also provides .cljs compatibility."
  (:require
   [clojure.set :as set]
   [clojure.spec.alpha :as spec]
   [clojure.walk :as walk]
   [formatting-stack.formatters.how-to-ns]
   [formatting-stack.protocols.formatter :as formatter]
   [formatting-stack.protocols.linter :as linter]
   [formatting-stack.util :refer [ensure-coll ensure-sequential process-in-parallel! rcomp read-ns-decl]]
   [formatting-stack.util.diff :as diff :refer [diff->line-numbers]]
   [formatting-stack.util.ns :as util.ns :refer [replace-ns-form! write-ns-replacement!]]
   [medley.core :refer [deep-merge]]
   [nedap.speced.def :as speced]
   [nedap.utils.modular.api :refer [implement]]
   [nedap.utils.spec.api :refer [check!]]))

(spec/def ::libspec coll?)

(spec/def ::libspecs (spec/coll-of ::libspec))

(speced/defn remove-refer [^::libspec libspec]
  (let [libspec (vec libspec)
        index-a (->> libspec
                     (keep-indexed (fn [i x]
                                     (when (#{:refer} x)
                                       i)))
                     (first))
        index-b (some-> index-a inc)]
    (if-not index-b
      libspec
      (->> (-> libspec
               (assoc index-a nil)
               (assoc index-b nil))
           (keep identity)
           (vec)))))

(speced/defn without-refer= [^::libspec x, ^::libspec y]
  (->> [x y]
       (map remove-refer)
       (apply =)))

(speced/defn ^::libspecs maybe-remove-optionless-libspecs [^::libspecs libspecs]
  (if (->> libspecs count #{0 1})
    libspecs
    (let [the-map (->> libspecs
                       (group-by (rcomp count #{1})))
          [optionless optionful] (map (partial get the-map)
                                      [1 nil])]
      (if (-> optionful count pos?)
        (distinct optionful)
        [(first optionless)]))))

(speced/defn ^::speced/nilable ^set? extract-refers [^::libspec libspec]
  (let [libspec (vec libspec)
        index (some->> libspec
                       (keep-indexed (fn [i x]
                                       (when (#{:refer} x)
                                         i)))
                       (first)
                       (inc))
        refers (when index
                 (get libspec index))]
    (when (coll? refers)
      (set refers))))

(speced/defn ^::libspecs maybe-remove-libspec-subsets [^::libspecs libspecs]
  {:pre [(let [lib (ffirst libspecs)]
           (check! (partial every? #{lib}) (map first libspecs)))
         (check! (partial not-any? set?) libspecs)]}
  (if (->> libspecs count #{0 1})
    libspecs
    (let [the-sets (atom [] :validator (fn [s]
                                         (check! (spec/coll-of set?) s)))]
      (->> libspecs
           ;; mapv, for ensuring `the-sets` is completely built:
           (mapv (fn [libspec]
                   (when-let [refers (extract-refers libspec)]
                     (swap! the-sets conj refers))
                   libspec))
           (filterv (fn [libspec]
                      (let [refers (extract-refers libspec)]
                        (cond
                          (not refers)
                          true

                          (and (->> @the-sets
                                    (remove #{refers})
                                    (some (fn [candidate]
                                            (set/subset? refers candidate))))
                               (->> libspecs
                                    (remove #{libspec})
                                    (some (partial without-refer= libspec))))
                          false

                          true
                          true))))))))

(speced/defn ^boolean? is-or-has-reader-conditional? [x]
  (or (reader-conditional? x)
      (and (coll? x)
           (let [result (atom false)]
             (->> x
                  (walk/postwalk (fn [item]
                                   (when (reader-conditional? item)
                                     (reset! result true))
                                   item)))
             @result))))

(speced/defn remove-exact-duplicates [^::util.ns/ns-form ns-form]
  (let [replacement (->> ns-form
                         (walk/postwalk (fn [x]
                                          (if-not (and (sequential? x)
                                                       (#{:require :require-macros} (first x)))
                                            x
                                            (let [{reader-conditionals true
                                                   normal-forms        false} (->> x
                                                                                   rest
                                                                                   (group-by is-or-has-reader-conditional?))
                                                  reader-conditionals (->> reader-conditionals
                                                                           (distinct)
                                                                           (sort-by coll?))
                                                  normal-forms (->> normal-forms
                                                                    (map ensure-coll)
                                                                    (group-by first)
                                                                    (mapcat (fn [[_ libspecs]]
                                                                              (->> libspecs
                                                                                   distinct
                                                                                   maybe-remove-optionless-libspecs
                                                                                   maybe-remove-libspec-subsets))))
                                                  all-forms (concat normal-forms reader-conditionals)]
                                              (apply list (first x) all-forms))))))]
    (when-not (= replacement ns-form)
      replacement)))

(speced/defn ^{::speced/spec (complement #{"nil"})} duplicate-cleaner [ns-form]
  (some-> ns-form remove-exact-duplicates pr-str))

(defn replaceable-ns-form
  [how-to-ns-opts filename]
  (when (read-ns-decl filename)
    (util.ns/replaceable-ns-form filename duplicate-cleaner how-to-ns-opts)))

(defn format! [{:keys [how-to-ns-opts]} files]
  (->> files
       (process-in-parallel! (fn [filename]
                               (when-let [replacement (replaceable-ns-form how-to-ns-opts filename)]
                                 (println "Removing trivial duplicates in `ns` form:" filename)
                                 (write-ns-replacement! filename replacement)))))
  nil)

(defn lint! [{:keys [how-to-ns-opts]} files]
  (->> files
       (process-in-parallel! (fn [filename]
                               (when-let [{:keys [final-ns-form-str
                                                  original-ns-form-str]}
                                          (replaceable-ns-form how-to-ns-opts filename)]
                                 (let [diff (diff/unified-diff filename original-ns-form-str final-ns-form-str)]
                                   (->> (diff->line-numbers diff)
                                        (mapv (fn [{:keys [start]}]
                                                {:filename filename
                                                 :diff     diff
                                                 :msg      "Duplicate ns forms found"
                                                 :column   0
                                                 :line     start
                                                 :level    :warning
                                                 :source   :formatting-stack/trivial-ns-duplicates})))))))
       (filter some?)
       (mapcat ensure-sequential)))

(defn new [{:keys [how-to-ns-opts]
            :or   {how-to-ns-opts {}}}]
  (implement {:id ::id
              :how-to-ns-opts (deep-merge formatting-stack.formatters.how-to-ns/default-how-to-ns-opts how-to-ns-opts)}
    linter/--lint! lint!
    formatter/--format! format!))
