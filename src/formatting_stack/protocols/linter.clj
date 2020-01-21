(ns formatting-stack.protocols.linter
  (:require
   [formatting-stack.protocols.spec :as protocols.spec]
   [nedap.speced.def :as speced]))

(speced/defprotocol Linter
  "A specific member of the \"stack\", that lints files.

Normally it's a wrapper around a linting library, with extra configuration, performance improvements, etc."

  (^::protocols.spec/reports lint! [this, ^::protocols.spec/filenames filenames]
    "Lints `filenames` according to a linter of your choice: e.g. Eastwood, or Kibit, lein-dependency-check, etc."))
