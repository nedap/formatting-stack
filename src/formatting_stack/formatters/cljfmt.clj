(ns formatting-stack.formatters.cljfmt
  (:require
   [cljfmt.main]
   [formatting-stack.formatters.cljfmt.impl :as impl]
   [formatting-stack.indent-specs :refer [default-third-party-indent-specs]]
   [formatting-stack.protocols.formatter :as formatter]
   [formatting-stack.protocols.linter :as linter]
   [formatting-stack.util :refer [ensure-sequential process-in-parallel!]]
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
                               (let [indents (impl/cljfmt-indents-for filename third-party-indent-specs)
                                     {{:keys [okay]} :counts
                                      :keys [file diff]} (#'cljfmt.main/check-one {:indents indents} filename)]
                                 (when (zero? okay)
                                   (->> diff
                                        (re-seq #".*\@\@\ -(\d),(\d) \+\d,\d \@\@*") ;; fixme
                                        (map #(take-last 2 %))
                                        (mapv (fn [[start stop]]
                                                {:filename file
                                                 :diff diff
                                                 :level :warning
                                                 :column 1
                                                 :line (Long/parseLong start)
                                                 :msg (str "Indentation is wrong between " start "-" stop)
                                                 :source :cljfmt/indent})))))))
       (mapcat ensure-sequential)))

(speced/defn new [{:keys [third-party-indent-specs]
                   :or {third-party-indent-specs default-third-party-indent-specs}}]
  (implement {:id ::id
              :third-party-indent-specs third-party-indent-specs}
    formatter/--format! format!
    linter/--lint! lint!))
