(ns formatting-stack.compilers.refactor-nrepl
  "Warms up refactor-nrepl's 'AST cache', for a more efficient performance than the default.

  If your IDE/Editor doesn't use this Compiler, nothing bad will happen. You are free to disable it."
  (:require
   [clojure.pprint :as pprint]
   [formatting-stack.protocols.compiler]
   [refactor-nrepl.analyzer]))

(ns-unmap *ns* 'Compiler)

(defrecord Compiler []
  formatting-stack.protocols.compiler/Compiler
  (compile! [_ _]
    (let [result (refactor-nrepl.analyzer/warm-ast-cache)
          ok? (->> result
                   (partition-all 2)
                   (map second)
                   (distinct)
                   (every? #{"OK"}))]
      (when-not ok?
        (println ::Compiler "AST cache warming failed:")
        (pprint/pprint result)))))
