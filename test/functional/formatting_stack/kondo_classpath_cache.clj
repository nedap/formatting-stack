(ns functional.formatting-stack.kondo-classpath-cache
  (:require
   [clojure.test :refer [are deftest is testing]]
   [formatting-stack.kondo-classpath-cache :as sut]
   [formatting-stack.util :refer [rcomp]]))

(deftest runner
  (testing "All choices have identical arglists (and therefore can be swapped transparently)"
    (let [arglists (->> sut/runner-mapping vals (map (rcomp find-var meta :arglists)))]
      (assert (seq arglists))
      (assert (every? some? arglists))
      (is (apply = arglists))))

  (let [proof (atom nil)]
    (are [input expected] (do
                            (reset! proof [])
                            (is (= expected
                                   input))
                            true)
      @(sut/runner (swap! proof conj 1))                      [1]
      @(sut/runner (swap! proof conj 1) (swap! proof conj 2)) [1 2])))
