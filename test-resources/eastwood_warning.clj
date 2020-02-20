(ns eastwood-warning)

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

(defn no-pre-post-warning--2
  "This function should not return a prepost warning (because the postcondition is dynamic)"
  []
  {:pre [x *dynamic*]} ;; variation: *dynamic* is not the first member
  x)

(defn no-pre-post-warning--3
  "This function should not return a prepost warning (because there's no precondition at all: the {:pre} is the return value)"
  [x]
  {:pre [x *dynamic*]})
