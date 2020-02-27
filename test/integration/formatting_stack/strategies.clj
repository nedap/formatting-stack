(ns integration.formatting-stack.strategies
  (:require
   [clojure.test :refer :all]
   [formatting-stack.strategies :as sut]))

(deftest all-files
  (testing "It runs without errors, exercising its specs"
    (let [n (->> (sut/all-files :files [])
                 (count))]
      (is (pos? n)))))

(deftest git-diff-against-default-branch
  (testing "It runs without errors, exercising its specs"
    (let [first-commit "ebb0ca8" ;; f-stack's first ever commit
          n (->> (sut/git-diff-against-default-branch :target-branch first-commit)
                 (count))]
      (is (pos? n)))))
