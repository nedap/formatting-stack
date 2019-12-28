(ns formatting-stack.protocols.linter
  (:require
   [nedap.speced.def :as speced]))

(speced/defprotocol Linter
  "" ;; FIXME
  (lint! [this ^coll? filenames]
    "Lints `filenames` according to a linter of your choice: e.g. Eastwood, or Kibit, lein-dependency-check, etc."))
