(ns formatting-stack.formatters.cider
  (:require
   [formatting-stack.protocols.formatter]))

(defrecord Formatter [options third-party-indent-specs]
  formatting-stack.protocols.formatter/Formatter
  (format! [this files]
    (doseq [[var-sym metadata] third-party-indent-specs]
      (some-> var-sym resolve (alter-meta! merge metadata)))))
