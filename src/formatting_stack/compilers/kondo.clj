(ns formatting-stack.compilers.kondo
  "Creates cache for entire classpath to make better use of the project-wide analysis capabilities.

   Excluded from the formatting-stack defaults:
   AST analysis can be quite slow (much more than a `clojure.tools.namespace.repl/refresh`),
   increasing the chances for concurrent (buggy) refreshing of Clojure namespaces."
  (:require
   [clj-kondo.core :as clj-kondo]
   [clojure.string :as str]
   [formatting-stack.protocols.compiler])
  (:import (java.io File)))

(ns-unmap *ns* 'Compiler)

(defrecord Compiler []
  formatting-stack.protocols.compiler/Compiler
  (compile! [_ _]
    (.mkdir (File. ".clj-kondo"))
    (clj-kondo/run! {:lint (-> (System/getProperty "java.class.path")
                               (str/split #"\:"))})
   nil))
