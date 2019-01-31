(ns formatting-stack.protocols.formatter)

(defprotocol Formatter
  (format! [this filenames]
    "The formatter should be `require`d dynamically.
e.g. if a namespace containing an implementation of this protocol should `require` cljfmt
(or whatever formatter it is) with `clojure.core/require`, not with `(:require ...)`.

`filenames` is a sequence of filenames"))
