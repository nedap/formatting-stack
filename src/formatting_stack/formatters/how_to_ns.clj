(ns formatting-stack.formatters.how-to-ns
  (:require
   [clojure.string :as str]
   [com.gfredericks.how-to-ns.main :as how-to-ns.main]
   [formatting-stack.protocols.formatter]
   [formatting-stack.util :refer [process-in-parallel!]]
   [medley.core :refer [deep-merge]]))

(def default-how-to-ns-opts {:require-docstring?      false
                             :sort-clauses?           true
                             ;; should be false, but https://git.io/fhMLm can break code:
                             :allow-refer-all?        true
                             :allow-extra-clauses?    false
                             :align-clauses?          false
                             :import-square-brackets? false})

(defrecord Formatter [options]
  formatting-stack.protocols.formatter/Formatter
  (format! [this files]
    (let [how-to-ns-files (remove #(str/ends-with? % ".edn") files)
          how-to-ns-opts (deep-merge default-how-to-ns-opts
                                     (or options {}))]
      (->> how-to-ns-files
           (process-in-parallel! (fn [filename]
                                   (how-to-ns.main/fix [filename] how-to-ns-opts)))))))
