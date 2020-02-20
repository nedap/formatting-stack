(ns eastwood-warning)

(def x (def y ::z))

(def reflection-warning
  (letfn [(get-path [x] (.getPath x))]
    (get-path (java.io.File. "a.clj"))))

(def ^:dynamic *dynamic*)
(defn no-pre-post-warning [x]
  {:pre [*dynamic*]}
  x)
