(ns integration.formatting-stack.strategies
  (:require
   [clojure.java.io :as io]
   [clojure.java.shell :refer [sh]]
   [clojure.set :as set]
   [clojure.string :as string]
   [clojure.test :refer [deftest is testing use-fixtures]]
   [clojure.tools.namespace.repl :as tools.namespace.repl]
   [formatting-stack.strategies :as sut]
   [formatting-stack.test-helpers :as test-helpers :refer [git-integration-dir]]
   [formatting-stack.util :refer [rcomp]]
   [nedap.speced.def :as speced])
  (:import
   (java.io File)))

(def ^File deletable-file (io/file git-integration-dir "test-resources" "deletable.clj"))

(def ^File rename-destination (io/file git-integration-dir "test-resources" "deletable_moved.clj"))

(def ^File creatable-filename (io/file git-integration-dir "test-resources" "creatable.clj"))

(def creatable-contents (pr-str '(ns foo)))

(defn cleanup-testing-repo! []
  (-> rename-destination .delete)
  (-> creatable-filename .delete)

  (sh "git" "reset" "--" "test-resources/deletable.clj")
  (sh "git" "reset" "--" "test-resources/deletable_moved.clj")
  (sh "git" "reset" "--" "test-resources/creatable.clj")
  (sh "git" "checkout" "test-resources/deletable.clj")

  (assert (-> deletable-file .exists))
  (assert (not (-> rename-destination .exists)))
  (assert (not (-> creatable-filename .exists))))

(use-fixtures :once
  test-helpers/with-git-repo

  (fn [t]
    (assert (-> deletable-file .exists))

    (assert (not (-> rename-destination .exists)))

    (assert (not (-> creatable-filename .exists)))

    (t)))

(defn assert-pristine-git-status! []
  (assert (-> (sh "git" "status" "--porcelain")
              :out
              #{""})
          "This test needs the `git status` to be pristine; else it will fail and/or destroy your WIP."))

(defn expect-sane-output! [corpus]
  (let [n (count corpus)]
    (is (pos? n))
    (doseq [^String filename corpus
            :let [f (io/file filename)]]
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
        (cleanup-testing-repo!))))

  (assert-pristine-git-status!)

  (testing "It runs without errors, exercising its specs, even in face of files staged for deletion"
    (try
      (-> deletable-file .delete)
      (sh "git" "add" "-A")
      (expect-sane-output! (sut/all-files :files []))
      (finally
        (cleanup-testing-repo!))))

  (assert-pristine-git-status!)

  (testing "It runs without errors, exercising its specs, even in face of renamed (but not staged) files"
    (try
      (-> deletable-file (.renameTo rename-destination))
      (expect-sane-output! (sut/all-files :files []))
      (finally
        (cleanup-testing-repo!))))

  (assert-pristine-git-status!)

  (testing "It runs without errors, exercising its specs, even in face of files staged for renaming"
    (try
      (-> deletable-file (.renameTo rename-destination))
      (sh "git" "add" "-A")
      (expect-sane-output! (sut/all-files :files []))
      (finally
        (cleanup-testing-repo!)))))

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
        (cleanup-testing-repo!))))

  (assert-pristine-git-status!)

  (testing "It runs without errors, exercising its specs, even in face of files staged for deletion"
    (try
      (-> deletable-file .delete)
      (sh "git" "add" "-A")
      (expect-sane-output! (sut/git-diff-against-default-branch :target-branch @root-commit))
      (finally
        (cleanup-testing-repo!))))

  (assert-pristine-git-status!)

  (testing "It runs without errors, exercising its specs, even in face of renamed (but not staged) files"
    (try
      (-> deletable-file (.renameTo rename-destination))
      (expect-sane-output! (sut/git-diff-against-default-branch :target-branch @root-commit))
      (finally
        (cleanup-testing-repo!))))

  (assert-pristine-git-status!)

  (testing "It runs without errors, exercising its specs, even in face of files staged for renaming"
    (try
      (-> deletable-file (.renameTo rename-destination))
      (sh "git" "add" "-A")
      (expect-sane-output! (sut/git-diff-against-default-branch :target-branch @current-commit))
      (finally
        (cleanup-testing-repo!))))

  (assert-pristine-git-status!)

  (testing "It runs without errors, exercising its specs, even in face of an ambiguous target-branch"
    (let [ambiguous-file (io/file git-integration-dir @root-commit)]
     (try
       ;; creating a file with a filename which collides with a sha.
       (spit ambiguous-file creatable-contents)
       (expect-sane-output! (sut/git-diff-against-default-branch :target-branch @root-commit))
       (finally
         (sh "git" "reset" "--" @root-commit)
         (-> ambiguous-file .delete)
         (cleanup-testing-repo!))))))

(deftest git-not-completely-staged

  (assert-pristine-git-status!)

  (testing "Non-completely-added files show up"
    (try
      (spit creatable-filename creatable-contents)

      (is (= [(-> creatable-filename .getCanonicalPath)]
             (sut/git-not-completely-staged :files [])))

      (testing "Added files don't show up"
        (try
          (sh "git" "add" "-A")
          (is (= []
                 (sut/git-not-completely-staged :files [])))
          (finally
            (sh "git" "reset" "."))))

      (finally
        (cleanup-testing-repo!))))

  (assert-pristine-git-status!)

  (testing "It can gracefully handle the presence of files partially staged for deletion"
    (try
      (-> deletable-file .delete)
      (is (sut/git-not-completely-staged :files []))
      (finally
        (cleanup-testing-repo!)))))

(deftest git-completely-staged

  (assert-pristine-git-status!)

  (testing "Non-completely-added files don't show up"
    (try
      (spit creatable-filename creatable-contents)

      (is (= []
             (sut/git-completely-staged :files [])))

      (testing "Added files show up"
        (try
          (sh "git" "add" "-A")
          (is (= [(-> creatable-filename .getCanonicalPath)]
                 (sut/git-completely-staged :files [])))
          (finally
            (sh "git" "reset" "."))))

      (finally
        (cleanup-testing-repo!))))

  (assert-pristine-git-status!)

  (testing "It can gracefully handle the presence of files staged for deletion"
    (try
      (-> deletable-file .delete)
      (sh "git" "add" (str deletable-file))
      (is (sut/git-completely-staged :files []))
      (finally
        (cleanup-testing-repo!))))

  (testing "It can gracefully handle the presence of files staged for renaming"
    (try
      (-> deletable-file (.renameTo rename-destination))
      (sh "git" "add" "-A")
      (is (sut/git-completely-staged :files []))
      (finally
        (cleanup-testing-repo!)))))

(deftest namespaces-within-refresh-dirs-only
  (speced/let [^{::speced/spec (rcomp count (partial < 100))}
               all-files (sut/all-files :files [])
               refresh-dirs (->> tools.namespace.repl/refresh-dirs
                                 (mapv (rcomp (partial io/file git-integration-dir)
                                              str)))
               result (sut/namespaces-within-refresh-dirs-only :files all-files
                                                               :refresh-dirs refresh-dirs)]
    (is (seq result)
        "Returns non-empty results (since f-s itself has namespaces within `src`, `test`, etc)")

    (is (< (count result)
           (count all-files))
        "Doesn't include files outside the refresh dirs")

    (is (set/subset? (set result)
                     (set all-files))
        "Is a subtractive (and not additive) mechanism")))
