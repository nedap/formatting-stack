(ns unit.formatting-stack.strategies.impl
  (:require
   [clojure.test :refer [are deftest is testing]]
   [formatting-stack.strategies.impl :as sut]))

(deftest deleted-file?
  (are [input expected] (testing input
                          (is (= expected
                                 (sut/deleted-file? input)))
                          true)
    "D "                 true
    " D "                true
    "DD "                true
    "DM "                true
    "DU "                true
    "AD "                true
    "CD "                true
    "MD "                true
    "RD "                true
    "UD "                true

    ""                   false
    "?? "                false
    " M "                false
    "M  "                false
    "MM "                false
    "A  "                false
    "AM "                false
    "R  "                false
    "RM "                false
    "C  "                false
    "CM "                false
    "UU "                false
    "AA "                false
    "AU "                false
    "UA "                false

    "../src/foo/bar.clj" false
    "src/foo/bar.clj"    false))

(deftest remove-deletion-markers
  (are [input] (testing input
                 (let [filename "a.clj"
                       complete (str input filename)]
                   (is (= filename
                          (sut/remove-deletion-markers complete)))
                   true))
    "D "
    " D "
    "DD "
    "DM "
    "DU "
    "AD "
    "CD "
    "MD "
    "RD "
    "UD "))
