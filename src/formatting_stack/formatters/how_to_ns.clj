(ns formatting-stack.formatters.how-to-ns
  (:require
   [clojure.string :as str]
   [com.gfredericks.how-to-ns.main :as how-to-ns.main]
   [formatting-stack.protocols.formatter :as formatter]
   [formatting-stack.util :refer [process-in-parallel!]]
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
  (->> (remove #(str/ends-with? % ".edn") files)
       (process-in-parallel! (fn [filename]
                               (how-to-ns.main/fix [filename] how-to-ns-options)))))

(defn new [{:keys [how-to-ns-options]
            :or {how-to-ns-options {}}}]
  (implement {:how-to-ns-options (deep-merge formatting-stack.formatters.how-to-ns/default-how-to-ns-opts how-to-ns-options)}
   formatter/--format! format!))
