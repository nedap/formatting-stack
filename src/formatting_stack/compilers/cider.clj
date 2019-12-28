(ns formatting-stack.compilers.cider
  (:require
   [formatting-stack.protocols.compiler :as compiler]
   [nedap.utils.modular.api :refer [implement]]))

(defn compile! [{:keys [third-party-indent-specs]} _files]
  (doseq [[var-sym metadata] third-party-indent-specs]
    (some-> var-sym resolve (alter-meta! merge metadata))))

(defn new [{:keys [third-party-indent-specs] :as options}]
  (implement options
    compiler/--compile! compile!))
