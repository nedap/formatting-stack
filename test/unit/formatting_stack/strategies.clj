(ns unit.formatting-stack.strategies
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is use-fixtures]]
   [formatting-stack.strategies :as sut]
   [formatting-stack.strategies.impl :as sut.impl]
   [formatting-stack.strategies.impl.git-status :as git-status]))

(use-fixtures :once (fn [tests]
                      (binding [sut.impl/*filter-existing-files?* false
                                sut.impl/*skip-existing-files-check?* true]
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
    (->> files
         (map strip)
         (sut.impl/absolutize "git"))))

(deftest git-completely-staged
  (is (= (strip-git completely-staged-files)
         (sut/git-completely-staged :files [] :impl all-files))))

(deftest git-not-completely-staged
  (let [expected (->> not-completely-staged-files
                      (remove git-status/deleted-file?)
                      (strip-git))]
    (assert (-> expected count pos?))
    (is (= expected
           (sut/git-not-completely-staged :files [] :impl all-files)))))

(deftest git-diff-against-default-branch
  (is (= (sut.impl/absolutize "git" ["a.clj"])
         (sut/git-diff-against-default-branch :files []
                                              :impl ["a.clj" "b.clj"]
                                              :blacklist (sut.impl/absolutize "git" ["b.clj"])))))
