(ns formatting-stack.formatters.cljfmt
  (:require
   [cljfmt.main]
   [formatting-stack.formatters.cljfmt.impl :as impl]
   [formatting-stack.indent-specs :refer [default-third-party-indent-specs]]
   [formatting-stack.protocols.formatter :as formatter]
   [formatting-stack.protocols.linter :as linter]
   [formatting-stack.util :refer [diff->line-numbers ensure-sequential process-in-parallel!]]
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
                                      :keys [diff]} (#'cljfmt.main/check-one {:indents indents} filename)]
                                 (when (zero? okay)
                                   (->> (diff->line-numbers diff)
                                        (mapv (fn [{:keys [begin end]}]
                                                {:filename filename
                                                 :diff diff
                                                 :level :warning
                                                 :column 0
                                                 :line begin
                                                 :msg (str "Indentation is wrong between " begin "-" end)
                                                 :source :cljfmt/indent})))))))
       (mapcat ensure-sequential)))

(speced/defn new [{:keys [third-party-indent-specs]
                   :or {third-party-indent-specs default-third-party-indent-specs}}]
  (implement {:id ::id
              :third-party-indent-specs third-party-indent-specs}
    formatter/--format! format!
    linter/--lint! lint!))
