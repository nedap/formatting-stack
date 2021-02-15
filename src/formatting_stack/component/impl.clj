(ns formatting-stack.component.impl
  (:require
   [formatting-stack.util :refer [rcomp]]
   [nedap.speced.def :as speced]))

(speced/defn parse-options [^map? this]
  (into {}
        (remove (rcomp val nil?))
        this))
