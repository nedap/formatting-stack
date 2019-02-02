(ns formatting-stack.protocols.linter)

(defprotocol Linter
  (lint! [this filenames]
    "Lints `filenames` according to a linter of your choice: e.g. Eastwood, or Kibit, lein-dependency-check, etc."))
