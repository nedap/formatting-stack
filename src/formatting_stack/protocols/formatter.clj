(ns formatting-stack.protocols.formatter
  (:require
   [nedap.speced.def :as speced]))

(speced/defprotocol Formatter
  "" ;; FIXME
  (format! [this ^coll? filenames]
    "Formats `filenames` according to a formatter of your choice."))
