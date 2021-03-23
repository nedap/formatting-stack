(ns functional.formatting-stack.formatters.clean-ns.impl
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer [are deftest]]
   [formatting-stack.formatters.clean-ns.impl :as sut]))

(deftest has-duplicate-requires?
  (are [input assertion] (let [file (io/file "test" "functional" "formatting_stack" "formatters" "clean_ns" input)
                               _ (assert (-> file .exists))
                               filename (-> file .getCanonicalPath)
                               result (sut/has-duplicate-requires? filename)]
                           (case assertion
                             :has-duplicates result
                             :does-not-have-duplicates (not result)))
    "has_duplicates.clj"           :has-duplicates
    "has_duplicates_2.clj"         :has-duplicates
    "has_duplicates_3.clj"         :has-duplicates
    "impl.clj"                     :does-not-have-duplicates
    "does_not_have_duplicates.clj" :does-not-have-duplicates))
