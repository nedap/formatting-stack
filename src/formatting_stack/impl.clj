(ns formatting-stack.impl)

(defn setup-cider-indents! [third-party-intent-specs]
  (doseq [[var-sym metadata] third-party-intent-specs]
    (some-> var-sym resolve (alter-meta! merge metadata))))
