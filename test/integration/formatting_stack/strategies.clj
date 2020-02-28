(ns integration.formatting-stack.strategies
  (:require
   [clojure.java.shell :refer [sh]]
   [clojure.test :refer [deftest is testing]]
   [formatting-stack.strategies :as sut])
  (:import
   (java.io File)))

(def ^File deletable-file (File. "test-resources/deletable.clj"))

(def ^String createable-filename "test-resources/createable.clj")

(def creatable-contents (pr-str '(ns foo)))

(assert (-> deletable-file .exists))

(assert (not (-> createable-filename File. .exists)))

(defn assert-pristine-git-status! []
  (assert (-> (sh "git" "status" "--porcelain")
              :out
              #{""})
          "This test needs the `git status` to be pristine; else it will fail and/or destroy your WIP."))

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

  (assert-pristine-git-status!)

  (testing "It runs without errors, exercising its specs, even in face of deleted (but not staged) files"
    (try
      (-> deletable-file .delete)
      (expect-sane-output! (sut/all-files :files []))
      (finally
        (sh "git" "checkout" (str deletable-file))
        (assert (-> deletable-file .exists)))))

  (assert-pristine-git-status!)

  (testing "It runs without errors, exercising its specs, even in face of files staged for deletion"
    (try
      (-> deletable-file .delete)
      (sh "git" "add" "-A")
      (expect-sane-output! (sut/all-files :files []))
      (finally
        (sh "git" "reset" "--" (str deletable-file))
        (sh "git" "checkout" (str deletable-file))
        (assert (-> deletable-file .exists))))))

(deftest git-diff-against-default-branch

  (assert-pristine-git-status!)

  (testing "It runs without errors, exercising its specs, even in face of deleted (but not staged) files"
    (let [;; f-stack's first ever commit. This ensures a large, diverse corpus:
          first-commit "ebb0ca8"]

      (try
        (-> deletable-file .delete)
        (expect-sane-output! (sut/git-diff-against-default-branch :target-branch first-commit))
        (finally
          (sh "git" "checkout" (str deletable-file))
          (assert (-> deletable-file .exists))))))

  (assert-pristine-git-status!)

  (testing "It runs without errors, exercising its specs, even in face of files staged for deletion"
    (let [;; f-stack's first ever commit. This ensures a large, diverse corpus:
          first-commit "ebb0ca8"]

      (try
        (-> deletable-file .delete)
        (sh "git" "add" "-A")
        (expect-sane-output! (sut/git-diff-against-default-branch :target-branch first-commit))
        (finally
          (sh "git" "reset" "--" (str deletable-file))
          (sh "git" "checkout" (str deletable-file))
          (assert (-> deletable-file .exists)))))))

(deftest git-not-completely-staged

  (assert-pristine-git-status!)

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

  (assert-pristine-git-status!)

  (testing "It can gracefully handle the presence of files partially staged for deletion"
    (try
      (-> deletable-file .delete)
      (is (sut/git-not-completely-staged :files []))
      (finally
        (sh "git" "checkout" (str deletable-file))
        (assert (-> deletable-file .exists))))))

(deftest git-completely-staged

  (assert-pristine-git-status!)

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

  (assert-pristine-git-status!)

  (testing "It can gracefully handle the presence of files staged for deletion"
    (try
      (-> deletable-file .delete)
      (sh "git" "add" (str deletable-file))
      (is (sut/git-completely-staged :files []))
      (finally
        (sh "git" "reset" "--" (str deletable-file))
        (sh "git" "checkout" (str deletable-file))
        (assert (-> deletable-file .exists))))))
