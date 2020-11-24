(ns formatting-stack.formatters.how-to-ns
  (:require
   [com.gfredericks.how-to-ns :as how-to-ns]
   [com.gfredericks.how-to-ns.main :as how-to-ns.main]
   [formatting-stack.protocols.formatter :as formatter]
   [formatting-stack.protocols.linter :as linter]
   [formatting-stack.strategies :as strategies]
   [formatting-stack.util :refer [ensure-sequential process-in-parallel!]]
   [formatting-stack.util.diff :as diff :refer [diff->line-numbers]]
   [medley.core :refer [deep-merge]]
   [nedap.utils.modular.api :refer [implement]]))

(def default-how-to-ns-opts {:require-docstring?      false
                             :sort-clauses?           true
                             ;; should be false, but https://git.io/fhMLm can break code:
                             :allow-refer-all?        true
                             :allow-extra-clauses?    false
                             :align-clauses?          false
                             :import-square-brackets? false})

(defn format! [{:keys [how-to-ns-options]} files]
  (->> (strategies/exclude-edn :files files)
       (process-in-parallel! (fn [filename]
                               (how-to-ns.main/fix [filename] how-to-ns-options))))
  nil)

(defn lint! [{:keys [how-to-ns-options]} files]
  (->> (strategies/exclude-edn :files files)
       (process-in-parallel! (fn [filename]
                               (let [contents  (slurp filename)
                                     formatted (how-to-ns/format-initial-ns-str contents how-to-ns-options)]
                                 (when-not (= contents formatted)
                                   (let [diff (diff/unified-diff filename contents formatted)]
                                     (->> (diff->line-numbers diff)
                                          (mapv (fn [{:keys [start]}]
                                                  {:filename filename
                                                   :diff diff
                                                   :line start
                                                   :column 0
                                                   :level :warning
                                                   :msg "Detected unsorted, renamed or extra clauses in the ns format"
                                                   :warning-details-url "https://stuartsierra.com/2016/clojure-how-to-ns.html"
                                                   :source :how-to-ns/ns}))))))))
       (filter some?)
       (mapcat ensure-sequential)))

(defn new [{:keys [how-to-ns-options]
            :or   {how-to-ns-options {}}}]
  (implement {:id ::id
              :how-to-ns-options (deep-merge default-how-to-ns-opts how-to-ns-options)}
    linter/--lint! lint!
    formatter/--format! format!))
