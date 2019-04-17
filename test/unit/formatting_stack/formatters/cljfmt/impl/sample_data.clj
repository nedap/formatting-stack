(ns unit.formatting-stack.formatters.cljfmt.impl.sample-data
  (:refer-clojure :exclude [do]))

(defmacro foo
  {:style/indent 0}
  [])

(defmacro do
  {:style/indent 7}
  [])
