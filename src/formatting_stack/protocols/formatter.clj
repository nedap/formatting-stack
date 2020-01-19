(ns formatting-stack.protocols.formatter
  (:require
   [formatting-stack.protocols.spec :as protocols.spec]
   [nedap.speced.def :as speced]))

(speced/defprotocol Formatter
  "A specific member of the \"stack\", that format files.

Normally it's a wrapper around a formatting library, with extra configuration, performance improvements, etc."

  (format! [this, ^::protocols.spec/filenames filenames]
    "Formats `filenames` according to a formatter of your choice."))
