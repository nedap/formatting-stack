(ns formatting-stack.util
  (:require
   [clojure.tools.namespace.file :as file]
   [clojure.tools.namespace.parse :as parse]))

(defmacro rcomp
  "Like `comp`, but in reverse order.
  With it, members of `->>` chains can consistently be read left-to-right."
  [& fns]
  (cons `comp (reverse fns)))

(defn dissoc-by [m f]
  (->> m
       (filter (rcomp first f))
       (into {})))

(def ns-name-from-filename (rcomp file/read-file-ns-decl parse/name-from-ns-decl))
