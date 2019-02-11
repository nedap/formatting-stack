(ns formatting-stack.linters.eastwood
  (:require
   [clojure.string :as str]
   [eastwood.lint :as eastwood]
   [formatting-stack.protocols.linter]
   [formatting-stack.util :refer [ns-name-from-filename]]))

(defrecord Eastwood []
  formatting-stack.protocols.linter/Linter
  (lint! [this filenames]
    (let [namespaces (->> filenames
                          (remove #(str/ends-with? % ".edn"))
                          (map ns-name-from-filename))
          linters (remove #{:suspicious-test :unused-ret-vals}
                          eastwood/default-linters)
          result (->> (with-out-str
                        (eastwood/eastwood (-> eastwood/default-opts
                                               (assoc :linters linters
                                                      :namespaces namespaces))))
                      (str/split-lines)
                      (remove (fn [line]
                                (or (str/blank? line)
                                    (some (fn [re]
                                            (re-find re line))
                                          [#"== Eastwood"
                                           #"^dbg "
                                           #"Directories scanned"
                                           #"Entering directory"
                                           #".*wrong-pre-post.*\*.*\*" ;; False positives for dynamic vars https://git.io/fhQTx
                                           #"== Warnings"
                                           #"== Linting done"])))))]
      (when-not (every? (fn [line]
                          (str/starts-with? line "== Linting"))
                        result)
        (->> result (str/join "\n") println)))))
