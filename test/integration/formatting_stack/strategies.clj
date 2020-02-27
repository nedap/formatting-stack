(ns integration.formatting-stack.strategies
  (:require
   [clojure.java.shell :refer [sh]]
   [clojure.test :refer [deftest is testing]]
   [formatting-stack.strategies :as sut])
  (:import
   (java.io File)))

(defn expect-sane-output! [corpus]
  (let [n (count corpus)]
    (is (pos? n))
    (doseq [^String filename corpus
            :let [f (File. filename)]]
      (is (-> f .exists))
      (testing "It emits absolutized filenames"
        (is (= filename
               (-> f .getCanonicalPath)))))))

(deftest all-files
  (testing "It runs without errors, exercising its specs"

    (expect-sane-output! (sut/all-files :files []))))

(deftest git-diff-against-default-branch
  (testing "It runs without errors, exercising its specs"
    (let [first-commit "ebb0ca8" ;; f-stack's first ever commit. This ensures a large, diverse corpus
          corpus (sut/git-diff-against-default-branch :target-branch first-commit)]

      (expect-sane-output! (sut/git-diff-against-default-branch :target-branch first-commit)))))

(def ^File deletable-file (File. "test-resources/deletable.clj"))

(def ^String createable-filename "test-resources/createable.clj")

(def creatable-contents (pr-str '(ns foo)))

(assert (-> deletable-file .exists))

(assert (not (-> createable-filename File. .exists)))

;; NOTE: naturally, this test can fail if your git stataus contains any changes.
;; You may need to `git commit` your WIP for these tests to pass.
(deftest git-not-completely-staged

  (testing "Non-completely-added files show up"
    (try
      (spit createable-filename creatable-contents)

      (is (= [(-> createable-filename File. .getCanonicalPath)]
             (sut/git-not-completely-staged :files [])))

      (testing "Added files don't show up"
        (try
          (sh "git" "add" "-A")
          (is (= []
                 (sut/git-not-completely-staged :files [])))
          (finally
            (sh "git" "reset" "."))))

      (finally
        (-> createable-filename File. .delete))))

  (testing "It can gracefully handle the presence of files partially staged for deletion"
    (try
      (-> deletable-file .delete)
      (is (sut/git-not-completely-staged :files []))
      (finally
        (sh "git" "checkout" (str deletable-file))
        (assert (-> deletable-file .exists))))))

;; NOTE: naturally, this test can fail if your git stataus contains any changes.
;; You may need to `git commit` your WIP for these tests to pass.
(deftest git-completely-staged

  (testing "Non-ompletely-added files don't show up"
    (try
      (spit createable-filename creatable-contents)

      (is (= []
             (sut/git-completely-staged :files [])))

      (testing "Added files show up"
        (try
          (sh "git" "add" "-A")
          (is (= [(-> createable-filename File. .getCanonicalPath)]
                 (sut/git-completely-staged :files [])))
          (finally
            (sh "git" "reset" "."))))

      (finally
        (-> createable-filename File. .delete))))

  (testing "It can gracefully handle the presence of files staged for deletion"
    (try
      (-> deletable-file .delete)
      (sh "git" "add" (str deletable-file))
      (is (sut/git-completely-staged :files []))
      (finally
        (sh "git" "reset" "--" (str deletable-file))
        (sh "git" "checkout" (str deletable-file))
        (assert (-> deletable-file .exists))))))
