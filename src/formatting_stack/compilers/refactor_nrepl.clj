(ns formatting-stack.compilers.refactor-nrepl
  "Warms up refactor-nrepl's 'AST cache', for a more efficient performance than the default.

  Excluded from the foramtting-stack defaults:
  AST analysis can be quite slow (much more than a `clojure.tools.namespace.repl/refresh`),
  increasing the chances for concurrent (buggy) refreshing of Clojure namespaces."
  (:require
   [clojure.pprint :as pprint]
   [formatting-stack.protocols.compiler :as compiler]
   [nedap.utils.modular.api :refer [implement]]
   [refactor-nrepl.analyzer]))

(defn compile! [_ _]
    (let [result (refactor-nrepl.analyzer/warm-ast-cache)
          ok? (->> result
                   (partition-all 2)
                   (map second)
                   (distinct)
                   (every? #{"OK"}))]
      (when-not ok?
        (println ::Compiler "AST cache warming failed:")
        (pprint/pprint result))))

(defn new []
  (implement {}
    compiler/--compile! compile!))
