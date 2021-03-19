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

;; * the "worker" source-path must be excluded.
;; * if updating this, please check if `formatting-stack.global-test-setup` also needs updating.
(set-refresh-dirs "src" "test" "dev")

(defn prepare-tests []
  (clear)
  (alter-var-root #'clojure.test/*load-tests* (constantly true))
  (refresh))

(defn suite []
  (prepare-tests)
  (run-all-tests #".*\.formatting-stack.*"))

(defn unit []
  (prepare-tests)
  (run-all-tests #"unit\.formatting-stack.*"))

(defn slow []
  (prepare-tests)
  (run-all-tests #"integration\.formatting-stack.*"))

(defn diff [x y]
  (-> x
      (lambdaisland.deep-diff/diff y)
      (lambdaisland.deep-diff/pretty-print)))
