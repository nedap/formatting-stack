(ns formatting-stack.report)

(def ^:dynamic *reporters*
  [#(println %)])

(defn report [report]
  (run! #(%1 report) *reporters*)
  report)
