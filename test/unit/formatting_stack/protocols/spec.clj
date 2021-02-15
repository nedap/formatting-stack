(ns unit.formatting-stack.protocols.spec
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.test :refer [are deftest is testing]]
   [formatting-stack.protocols.spec :as sut]))

(deftest members-spec
  (are [input expected] (testing input
                          (is (= expected
                                 (try
                                   (spec/valid? ::sut/members input)
                                   (catch AssertionError e
                                     (is (-> e
                                             .getMessage
                                             #{"Assert failed: Members should have unique ids\n(apply distinct? ids)"}))
                                     false))))
                          true)
    []                                                                           true
    [{:id :a}]                                                                   true
    [{:id :a} {:id :b}]                                                          true
    [{:id :a} {:id :b} {:id :a}]                                                 false
    [(reify) (reify) {:id :a} (reify) (reify) {:id :b}]                          true
    [(reify) (reify) {:id :a} (reify) (reify) {:id :b} (reify) (reify) {:id :a}] false))
