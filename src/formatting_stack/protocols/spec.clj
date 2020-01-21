(ns formatting-stack.protocols.spec
  (:require
   [clojure.spec.alpha :as spec]
   [nedap.utils.spec.predicates :refer [present-string?]]))

(spec/def ::filename present-string?)
(spec/def ::filenames (spec/coll-of ::filename))

(spec/def ::msg present-string?)
(spec/def ::linter ;; fixme, should be generic. ::source ?
  (fn [x]
    (and (keyword? x)
         (namespace x))))

(spec/def ::column
  (fn [x]
    (or (zero? x)
        (pos-int? x))))

(spec/def ::line ::column)
(spec/def ::level #{:warning :error :exception})

(defmulti reportmm :level)
(defmethod reportmm :exception [_]
  (spec/keys :req-un [::msg
                      ::level]))
(defmethod reportmm :default [_]
  (spec/keys :req-un [::filename
                      ::linter
                      ::msg
                      ::level
                      ::column
                      ::line]))

(spec/def ::report
  (spec/multi-spec reportmm :level))

(spec/def ::reports (spec/coll-of ::report))
