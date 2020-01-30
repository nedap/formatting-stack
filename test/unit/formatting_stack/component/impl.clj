(ns unit.formatting-stack.component.impl
  (:require
   [clojure.test :refer :all]
   [formatting-stack.component.impl :as sut]))

(deftest parse-options
  (are [input expected] (testing input
                          (is (= expected
                                 (sut/parse-options input)))
                          true)
    {}            ()
    {:a 1}        '(:a 1)
    {:a 1 :b nil} '(:a 1)))
