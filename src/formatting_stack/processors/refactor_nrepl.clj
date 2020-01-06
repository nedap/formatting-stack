(ns formatting-stack.processors.refactor-nrepl
  "Warms up refactor-nrepl's 'AST cache', for a more efficient performance than the default.

  Excluded from the foramtting-stack defaults:
  AST analysis can be quite slow (much more than a `clojure.tools.namespace.repl/refresh`),
  increasing the chances for concurrent (buggy) refreshing of Clojure namespaces."
  (:require
   [clojure.pprint :as pprint]
   [formatting-stack.protocols.processor :as processor]
   [nedap.utils.modular.api :refer [implement]]
   [refactor-nrepl.analyzer]))

(defn process! [_ _]
  (let [result (refactor-nrepl.analyzer/warm-ast-cache)
        ok? (->> result
                 (partition-all 2)
                 (map second)
                 (distinct)
                 (every? #{"OK"}))]
    (when-not ok?
      (println ::processor "AST cache warming failed:")
      (pprint/pprint result))))

(defn new []
  (implement {}
    processor/--process! process!))
