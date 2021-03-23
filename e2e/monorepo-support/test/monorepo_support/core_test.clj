(ns monorepo-support.core-test
  (:require
   [clojure.set :as set]
   [clojure.test :refer [are deftest is testing use-fixtures]]
   [formatting-stack.strategies])
  (:import
   (java.io File)))

(deftest e2e
  (let [all (set (formatting-stack.strategies/all-files :files []))
        staged (set (formatting-stack.strategies/git-completely-staged :files []))
        unstaged (set (formatting-stack.strategies/git-not-completely-staged :files []))
        staged+unstaged (into staged unstaged)]
    (is (seq all))
    (is (seq staged))
    (is (seq unstaged))

    (is (not= staged unstaged))
    (is (> (count all)
           (count staged+unstaged)))
    (is (set/superset? all staged+unstaged))

    (doseq [filename all
            :let [file (File. filename)
                  absolute (-> file .getCanonicalPath)]]
      (is (= filename
             absolute)
          "Returns absolutized filenames, which is important for monorepo support")

      (is (-> file .exists)
          "Said absolutized filenames reflect files that actually exist"))))
