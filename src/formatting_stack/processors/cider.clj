(ns formatting-stack.processors.cider
  (:require
   [formatting-stack.protocols.processor :as processor]
   [nedap.speced.def :as speced]
   [nedap.utils.modular.api :refer [implement]]
   [nedap.utils.spec.api :refer [check!]]))

(speced/defn process! [{:keys [^map? third-party-indent-specs]} _files]
  (doseq [[var-sym metadata] third-party-indent-specs]
    (check! symbol? var-sym
            map?    metadata)
    (some-> var-sym resolve (alter-meta! merge metadata))))

(defn new
  "This processor alters var metadata from third-party libs,
  so that runtime-based tooling such as CIDER can work more accurately."
  [{:keys [third-party-indent-specs] :as options}]
  (implement (assoc options :id ::id)
    processor/--process! process!))
