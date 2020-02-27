(ns integration.formatting-stack.strategies
  (:require
   [clojure.java.shell :refer [sh]]
   [clojure.test :refer [deftest is testing]]
   [formatting-stack.strategies :as sut])
  (:import
   (java.io File)))

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

(def ^File deletable-file (File. "test-resources/deletable.clj"))

(assert (-> deletable-file .exists))

(deftest git-not-completely-staged
  (testing "It can gracefully handle the presence of files partially staged for deletion"
    (try
      (-> deletable-file .delete)
      (is (sut/git-not-completely-staged :files []))
      (finally
        (sh "git" "checkout" (str deletable-file))
        (assert (-> deletable-file .exists))))))

(deftest git-completely-staged
  (testing "It can gracefully handle the presence of files staged for deletion"
    (try
      (-> deletable-file .delete)
      (sh "git" "add" (str deletable-file))
      (is (sut/git-completely-staged :files []))
      (finally
        (sh "git" "reset" "--" (str deletable-file))
        (sh "git" "checkout" (str deletable-file))
        (assert (-> deletable-file .exists))))))
