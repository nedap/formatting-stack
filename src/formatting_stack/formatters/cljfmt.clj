(ns formatting-stack.formatters.cljfmt
  (:require
   [cljfmt.main]
   [formatting-stack.formatters.cljfmt.impl :as impl]
   [formatting-stack.protocols.formatter :as formatter]
   [formatting-stack.protocols.linter :as linter]
   [formatting-stack.util :refer [process-in-parallel!]]
   [nedap.speced.def :as speced]
   [nedap.utils.modular.api :refer [implement]]))

(defn format! [{:keys [third-party-indent-specs]} files]
  (->> files
       (process-in-parallel! (fn [filename]
                               (let [indents (impl/cljfmt-indents-for filename third-party-indent-specs)]
                                 (cljfmt.main/fix [filename] {:indents indents})))))
  nil)

(defn lint! [{:keys [third-party-indent-specs]} files]
  (->> files
       (process-in-parallel! (fn [filename]
                               (let [indents (impl/cljfmt-indents-for filename third-party-indent-specs)]
                                 (#'cljfmt.main/check-one {:indents indents} filename))))
       (filter (fn [{{:keys [okay]} :counts}] (zero? okay)))
       (mapcat (fn [{:keys [file diff]}]
                 (->> diff
                   (re-seq #".*\@\@\ -(\d),(\d) \+\d,\d \@\@*") ;; fixme
                   (map #(take-last 2 %))
                   (map (fn [[start stop]]
                          {:filename file
                           :diff diff
                           :level :warning
                           :msg (str "Indentation is wrong between " start "-" stop)
                           :source :cljfmt/indent})))))))

(speced/defn new [{:keys [third-party-indent-specs] :as options}]
  (implement (assoc options :id ::id)
    formatter/--format! format!
    linter/--lint! lint!))
