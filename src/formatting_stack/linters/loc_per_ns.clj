(ns formatting-stack.linters.loc-per-ns
  (:require
   [clojure.string :as string]
   [formatting-stack.protocols.linter]
   [formatting-stack.util :refer [process-in-parallel!]]))

(defn overly-long-ns? [filename threshold]
  (-> filename
      slurp
      (string/split #"\n")
      (count)
      (> threshold)))

(defrecord Linter [max-lines-per-ns]
  formatting-stack.protocols.linter/Linter
  (lint! [this filenames]
    (let [max-lines-per-ns (or max-lines-per-ns 350)]
      (->> filenames
           (process-in-parallel! (fn [filename]
                                   (when (overly-long-ns? filename max-lines-per-ns)
                                     (println "Warning:"
                                              filename
                                              "is longer than"
                                              max-lines-per-ns
                                              "LOC. Consider refactoring."))))))))
