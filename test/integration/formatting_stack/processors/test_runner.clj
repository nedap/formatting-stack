(ns integration.formatting-stack.processors.test-runner
  (:require
   [formatting-stack.processors.test-runner :as sut]
   [clojure.test :refer :all]))

(deftest test!
  (testing "asserts *load-tests* is true"
    (binding [*load-tests* false]
      (is (thrown-with-msg? AssertionError #"clojure.test/\*load-tests\*"
                            (sut/test! :target-branch "main"))))))
