(ns formatting-stack.protocols.spec
  (:require
   [clojure.spec.alpha :as spec]
   [nedap.utils.spec.predicates :refer [present-string?]]))

(spec/def ::filename present-string?)

(spec/def ::filenames (spec/coll-of ::filename))

(spec/def ::msg present-string?)

(spec/def ::source
  (fn [x]
    (and (keyword? x)
         (namespace x))))

(spec/def ::column nat-int?)

(spec/def ::line ::column)

(spec/def ::level #{:warning :error :exception})

(defmulti reportmm :level)

(defmethod reportmm :exception [_]
  (spec/keys :req-un [::msg
                      ::exception
                      ::source
                      ::level]
             :opt-un [::filename]))

(defmethod reportmm :default [_]
  (spec/keys :req-un [::filename
                      ::source
                      ::msg
                      ::level
                      ::column
                      ::line]))

(spec/def ::report
  (spec/multi-spec reportmm :level))

(spec/def ::reports (spec/coll-of ::report))
