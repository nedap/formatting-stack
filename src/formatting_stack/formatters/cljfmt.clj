(ns formatting-stack.formatters.cljfmt
  (:require
   [cljfmt.core :as cljfmt]
   [cljfmt.main]
   [clojure.string :as string]
   [formatting-stack.formatters.cljfmt.impl :as impl]
   [formatting-stack.indent-specs :refer [default-third-party-indent-specs]]
   [formatting-stack.protocols.formatter :as formatter]
   [formatting-stack.protocols.linter :as linter]
   [formatting-stack.util :refer [ensure-sequential process-in-parallel!]]
   [formatting-stack.util.diff :as diff :refer [diff->line-numbers]]
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
       (process-in-parallel!
        (fn [filename]
          (let [indents (impl/cljfmt-indents-for filename third-party-indent-specs)
                original (slurp filename)
                revised ((cljfmt/wrap-normalize-newlines #(cljfmt/reformat-string % {:indents indents})) original)] ;; taken from https://git.io/Jkot7
            (when (not= original revised)
              (let [diff (diff/unified-diff filename original revised)]
                (->> (diff->line-numbers diff)
                     (mapv (fn [{:keys [start end]}]
                             {:filename filename
                              :diff     diff
                              :level    :warning
                              :column   0
                              :line     start
                              :msg      (str "Indentation or whitespace is wrong at " (->> (dedupe [start end])
                                                                                           (string/join "-")))
                              :source   :cljfmt/indent}))))))))
       (filter some?)
       (mapcat ensure-sequential)))

(speced/defn new [{:keys [third-party-indent-specs]
                   :or {third-party-indent-specs default-third-party-indent-specs}}]
  (implement {:id ::id
              :third-party-indent-specs third-party-indent-specs}
    formatter/--format! format!
    linter/--lint! lint!))
