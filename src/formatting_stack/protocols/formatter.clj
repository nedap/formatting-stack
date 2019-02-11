(ns formatting-stack.protocols.formatter)

(defprotocol Formatter
  (format! [this filenames]
    "Formats `filenames` according to a formatter of your choice."))
