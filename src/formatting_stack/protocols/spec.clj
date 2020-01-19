(ns formatting-stack.protocols.spec
  (:require
   [clojure.spec.alpha :as spec]
   [nedap.utils.spec.predicates :refer [present-string?]]))

(spec/def ::filenames (spec/coll-of present-string?))
