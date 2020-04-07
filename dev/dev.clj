(ns dev
  (:require
   [clj-java-decompiler.core :refer [decompile]]
   [clojure.java.javadoc :refer [javadoc]]
   [clojure.pprint :refer [pprint]]
   [clojure.repl :refer [apropos dir doc find-doc pst source]]
   [clojure.test :refer [run-all-tests run-tests]]
   [clojure.tools.namespace.repl :refer [clear refresh refresh-dirs set-refresh-dirs]]
   [criterium.core :refer [quick-bench]]
   [lambdaisland.deep-diff]))

(set-refresh-dirs "src" "dev" "test")


(defn suite []
  (refresh)
  (run-all-tests #".*\.formatting-stack.*"))

(defn unit []
  (refresh)
  (run-all-tests #"unit\.formatting-stack.*"))

(defn slow []
  (refresh)
  (run-all-tests #"integration\.formatting-stack.*"))

(defn diff [x y]
  (-> x
      (lambdaisland.deep-diff/diff y)
      (lambdaisland.deep-diff/pretty-print)))
