(ns integration.formatting-stack.strategies
  (:require
   [clojure.java.shell :refer [sh]]
   [clojure.string :as string]
   [clojure.test :refer [deftest is testing]]
   [formatting-stack.strategies :as sut])
  (:import
   (java.io File)))

(def ^File deletable-file (File. "test-resources/deletable.clj"))

(def ^File rename-destination (File. "test-resources/deletable_moved.clj"))

(def ^String createable-filename "test-resources/createable.clj")

(def creatable-contents (pr-str '(ns foo)))

(assert (-> deletable-file .exists))

(assert (not (-> rename-destination .exists)))

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
        (assert (-> deletable-file .exists)))))

  (assert-pristine-git-status!)

  (testing "It runs without errors, exercising its specs, even in face of renamed (but not staged) files"
    (try
      (-> deletable-file (.renameTo rename-destination))
      (expect-sane-output! (sut/all-files :files []))
      (finally
        (sh "git" "checkout" (str deletable-file))
        (sh "rm" (str rename-destination))
        (assert (-> deletable-file .exists))
        (assert (not (-> rename-destination .exists))))))

  (assert-pristine-git-status!)

  (testing "It runs without errors, exercising its specs, even in face of files staged for renaming"
    (try
      (-> deletable-file (.renameTo rename-destination))
      (sh "git" "add" "-A")
      (expect-sane-output! (sut/all-files :files []))
      (finally
        (sh "git" "reset" "--" (str deletable-file))
        (sh "git" "reset" "--" (str rename-destination))
        (sh "git" "checkout" (str deletable-file))
        (sh "rm" (str rename-destination))
        (assert (-> deletable-file .exists))
        (assert (not (-> rename-destination .exists)))))))

(def root-commit
  "f-stack's first ever commit. This ensures a large, diverse corpus."
  (delay
    (-> (sh "git" "rev-list" "--max-parents=0" "HEAD")
        (:out)
        (string/split #"\n")
        (first)
        (doto assert))))

(def current-commit
  "f-stack's most recent commit."
  (delay
    (-> (sh "git" "rev-list" "HEAD^..HEAD")
        (:out)
        (string/split #"\n")
        (first)
        (doto assert))))

(deftest git-diff-against-default-branch

  (assert-pristine-git-status!)

  (testing "It runs without errors, exercising its specs, even in face of deleted (but not staged) files"
    (try
      (-> deletable-file .delete)
      (expect-sane-output! (sut/git-diff-against-default-branch :target-branch @root-commit))
      (finally
        (sh "git" "checkout" (str deletable-file))
        (assert (-> deletable-file .exists)))))

  (assert-pristine-git-status!)

  (testing "It runs without errors, exercising its specs, even in face of files staged for deletion"
    (try
      (-> deletable-file .delete)
      (sh "git" "add" "-A")
      (expect-sane-output! (sut/git-diff-against-default-branch :target-branch @root-commit))
      (finally
        (sh "git" "reset" "--" (str deletable-file))
        (sh "git" "checkout" (str deletable-file))
        (assert (-> deletable-file .exists)))))

  (assert-pristine-git-status!)

  (testing "It runs without errors, exercising its specs, even in face of renamed (but not staged) files"
    (try
      (-> deletable-file (.renameTo rename-destination))
      (expect-sane-output! (sut/git-diff-against-default-branch :target-branch @root-commit))
      (finally
        (sh "git" "checkout" (str deletable-file))
        (sh "rm" (str rename-destination))
        (assert (-> deletable-file .exists))
        (assert (not (-> rename-destination .exists))))))

  (assert-pristine-git-status!)

  (testing "It runs without errors, exercising its specs, even in face of files staged for renaming"
    (try
      (-> deletable-file (.renameTo rename-destination))
      (sh "git" "add" "-A")
      (expect-sane-output! (sut/git-diff-against-default-branch :target-branch @current-commit))
      (finally
        (sh "git" "reset" "--" (str deletable-file))
        (sh "git" "reset" "--" (str rename-destination))
        (sh "git" "checkout" (str deletable-file))
        (sh "rm" (str rename-destination))
        (assert (-> deletable-file .exists))
        (assert (not (-> rename-destination .exists)))))))

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

  (testing "Non-completely-added files don't show up"
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
        (assert (-> deletable-file .exists)))))

  (testing "It can gracefully handle the presence of files staged for renaming"
    (try
      (-> deletable-file (.renameTo rename-destination))
      (sh "git" "add" "-A")
      (is (sut/git-completely-staged :files []))
      (finally
        (sh "git" "reset" "--" (str deletable-file))
        (sh "git" "reset" "--" (str rename-destination))
        (sh "git" "checkout" (str deletable-file))
        (sh "rm" (str rename-destination))
        (assert (-> deletable-file .exists))
        (assert (not (-> rename-destination .exists)))))))