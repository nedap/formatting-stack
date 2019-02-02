(ns formatting-stack.protocols.compiler)

(ns-unmap *ns* 'Compiler)

(defprotocol Compiler
  (compile! [this filenames]
    "Performs a compilation according to a compiler of your choice: e.g. the ClojureScript compiler, or Garden, Stefon, etc.
You are free to ignore `filenames`, compiling the whole project instead."))
