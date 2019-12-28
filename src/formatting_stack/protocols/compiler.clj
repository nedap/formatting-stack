(ns formatting-stack.protocols.compiler
  (:require
   [nedap.speced.def :as speced]))

;; using #'Compiler yielded this warning;
;; Warning: protocol #'formatting-stack.protocols.compiler/Compiler is overwriting method --compile! of protocol Compiler
(speced/defprotocol CompilerVar
  "" ;; FIXME
  (compile! [this ^coll? filenames]
    "Performs a compilation according to a compiler of your choice: e.g. the ClojureScript compiler, or Garden, Stefon, etc.
You are free to ignore `filenames`, compiling the whole project instead."))
