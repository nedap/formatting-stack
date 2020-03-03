(ns eastwood-warning
  (:require
   [clojure.spec.alpha :as spec]))

(def x (def y ::z))

(def reflection-warning
  (letfn [(get-path [x] (.getPath x))]
    (get-path (java.io.File. "a.clj"))))

(def ^:dynamic *dynamic*)

(defn no-pre-post-warning--1
  "This function should not return a prepost warning (because the precondition is dynamic)"
  [x]
  {:pre [*dynamic*]}
  x)

(def logical-false false)

(defn no-pre-post-warning--2
  "This function should return 1 prepost warning about `#'logical-false` (because the postcondition is dynamic)"
  []
  {:pre [logical-false *dynamic*]} ;; variation: *dynamic* is not the first member
  logical-false)

(defn no-pre-post-warning--3
  "This function should not return a prepost warning (because there's no precondition at all: the {:pre} is the return value)"
  [x]
  {:pre [x *dynamic*]})

(defn no-pre-post-warning--4
  "This function should return no prepost warnings."
  [x]
  {:pre [x *dynamic*]} ;; variation: *dynamic* is not the first member
  logical-false)

;; This shouldn't raise any warnings. See https://github.com/jonase/eastwood/issues/337
(spec/coll-of any?)

;; This shouldn't raise any warnings. See https://github.com/jonase/eastwood/issues/336
(defmulti example-mm identity)
