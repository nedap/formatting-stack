(ns formatting-stack.linters.eastwood
  (:require
   [clojure.string :as str]
   [eastwood.lint :as eastwood]
   [formatting-stack.protocols.linter]
   [formatting-stack.util :refer [ns-name-from-filename]]
   [medley.core :refer [deep-merge]]))

(def default-eastwood-options
  ;; Avoid false positives or more-annoying-than-useful checks:
  (let [linters (remove #{:suspicious-test :unused-ret-vals :constant-test :wrong-tag}
                        eastwood/default-linters)]
    (-> eastwood/default-opts
        (assoc :linters linters))))

(def default-warnings-to-silence
  [#"== Eastwood"
   #"^dbg "
   #"Warning: protocol .* is overwriting function" ;; False positive with nedap.utils.speced
   #"Directories scanned"
   #"Entering directory"
   #".*wrong-pre-post.*\*.*\*" ;; False positives for dynamic vars https://git.io/fhQTx
   #"== Warnings"
   #"== Linting done"])

(defrecord Eastwood [eastwood-options warnings-to-silence]
  formatting-stack.protocols.linter/Linter
  (lint! [this filenames]
    (let [namespaces (->> filenames
                          (remove #(str/ends-with? % ".edn"))
                          (map ns-name-from-filename))
          options (deep-merge default-eastwood-options
                              (or eastwood-options {}))
          warnings-to-silence (or warnings-to-silence default-warnings-to-silence)
          result (->> (with-out-str
                        (eastwood/eastwood (-> options
                                               (assoc :namespaces namespaces))))
                      (str/split-lines)
                      (remove (fn [line]
                                (or (str/blank? line)
                                    (some (fn [re]
                                            (re-find re line))
                                          warnings-to-silence)))))]
      (when-not (every? (fn [line]
                          (str/starts-with? line "== Linting"))
                        result)
        (->> result (str/join "\n") println)))))
