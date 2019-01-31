(ns formatting-stack.protocols.formatter)

(defprotocol Formatter
  (format! [this filenames]
    "Formats `filenames` according to a formatter of your choice.

The underlying formatter should be `require`d dynamically.
e.g. if a namespace containing an implementation of this protocol should `require` cljfmt
(or whatever formatter it is) with `clojure.core/require`, not with `(:require ...)`."))
