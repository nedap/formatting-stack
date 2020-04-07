(ns unit.formatting-stack.reporters.blacklist
  (:require
   [clojure.test :refer :all]
   [formatting-stack.reporters.blacklist :as sut]))

(deftest ignored-reports->predicate
  (let [predicate (sut/ignored-reports->predicate [{:filename "a.clj"}
                                                   {:filename "b.clj" :column 0 :line 2}
                                                   {:source :a/b}
                                                   {:level :exception}])]
    (are [report expected] (= expected
                              (predicate report))
      {:filename "b.clj" :column 0 :line 2}       true
      {:filename "a.clj" :column 0 :line 2}       true
      {:filename "c.clj" :source :a/b :msg "a"}   true
      {:msg "hoi" :level :exception}              true

      {:level :warning}                           false
      {:filename "abc.clj"}                       false
      {:filename "b.clj"}                         false
      {:filename "b.clj" :column 0 :line 3}       false)))
