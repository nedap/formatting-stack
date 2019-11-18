(ns formatting-stack.formatters.trivial-ns-duplicates
  "This formatter removes 'trivial' duplicate libspecs from the `ns` form.

  Compensates for some `refactor-nrepl.clean-ns` intricacies, and also provides .cljs compatibility."
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.walk :as walk]
   [formatting-stack.formatters.clean-ns.impl :refer [ns-form-of]]
   [formatting-stack.formatters.how-to-ns]
   [formatting-stack.protocols.formatter]
   [formatting-stack.util :refer [ensure-coll process-in-parallel! rcomp]]
   [formatting-stack.util.ns :as util.ns :refer [replace-ns-form!]]
   [medley.core :refer [deep-merge]]
   [nedap.speced.def :as speced]))

(spec/def ::libspecs (spec/coll-of coll?))

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
                                                       (#{:require :require-macros :import} (first x)))
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
                                                                                   maybe-remove-optionless-libspecs))))
                                                  all-forms (concat normal-forms reader-conditionals)]
                                              (apply list (first x) all-forms))))))]
    (when-not (= replacement ns-form)
      replacement)))

(defrecord Formatter [how-to-ns-opts]
  formatting-stack.protocols.formatter/Formatter
  (format! [this files]
    (let [how-to-ns-opts (deep-merge formatting-stack.formatters.how-to-ns/default-how-to-ns-opts
                                     (or how-to-ns-opts {}))]
      (->> files
           (process-in-parallel! (fn [filename]
                                   (when (ns-form-of filename)
                                     (replace-ns-form! filename
                                                       (speced/fn ^{::speced/spec (complement #{"nil"})} [ns-form]
                                                         (some-> ns-form remove-exact-duplicates pr-str))
                                                       "Removing trivial duplicates in `ns` form:"
                                                       how-to-ns-opts))))))))
