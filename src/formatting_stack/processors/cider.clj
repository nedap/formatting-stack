(ns formatting-stack.processors.cider
  (:require
   [formatting-stack.protocols.processor :as processor]
   [nedap.utils.modular.api :refer [implement]]))

(defn process! [{:keys [third-party-indent-specs]} _files]
  (doseq [[var-sym metadata] third-party-indent-specs]
    (some-> var-sym resolve (alter-meta! merge metadata))))

(defn new [{:keys [third-party-indent-specs] :as options}]
  (implement options
    processor/--process! process!))
