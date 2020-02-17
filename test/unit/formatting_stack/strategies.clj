(ns unit.formatting-stack.strategies
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is use-fixtures]]
   [formatting-stack.strategies :as sut]
   [formatting-stack.strategies.impl :as sut.impl]))

(use-fixtures :once (fn [tests]
                      (binding [sut.impl/*filter-existing-files?* false]
                        (tests))))

(def not-completely-staged-files
  ["AM test/unit/formatting_stack/a.clj"
   "MM test/unit/formatting_stack/a.clj"
   "AD test/unit/formatting_stack/b.clj"
   " M test/unit/formatting_stack/c.clj"
   " D test/unit/formatting_stack/d.clj"
   "?? test/unit/formatting_stack/e.clj"])

(def completely-staged-files
  ["M  test/unit/formatting_stack/f.clj"
   "R  test/unit/formatting_stack/g.clj -> test/unit/formatting_stack/G.clj"
   "A  test/unit/formatting_stack/h.clj"])

(def all-files (into not-completely-staged-files completely-staged-files))

(defn strip-git [files]
  (letfn [(strip [s]
            (-> s
                (str/replace #".* " "")
                (str/replace "test/unit/formatting_stack/g.clj -> " "")))]
    (map strip files)))

(deftest git-completely-staged
  (is (= (strip-git completely-staged-files)
         (sut/git-completely-staged :files [] :impl all-files))))

(deftest git-not-completely-staged
  (is (= (strip-git not-completely-staged-files)
         (sut/git-not-completely-staged :files [] :impl all-files))))

(deftest git-diff-against-default-branch
  (is (= ["a.clj"]
         (sut/git-diff-against-default-branch :files []
                                              :impl ["a.clj" "b.clj"]
                                              :blacklist ["b.clj"]))))
