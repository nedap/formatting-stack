(ns unit.formatting-stack.strategies.impl.git-diff
  (:require
   [clojure.test :refer [are deftest is testing]]
   [formatting-stack.strategies.impl.git-diff :as sut]))

(deftest deletion?
  (are [input expected] (testing input
                          (is (= expected
                                 (sut/deletion? input)))
                          true)
    ""          false
    "D\t"       true
    "D\t a.clj" true))

(deftest remove-markers
  (are [input expected] (testing input
                          (is (= expected
                                 (sut/remove-markers input)))
                          true)

    ;; `A` is replaced:

    "A\t"        ""
    "A\tfoo.clj" "foo.clj"
    "foo.cljA\t" "foo.cljA\t"

    ;; `B` is replaced (and so on for other letters - we're not testing a whole regex range)

    "B\t"        ""
    "B\tfoo.clj" "foo.clj"
    "foo.cljB\t" "foo.cljB\t"

    ;; merge conflicts are replaced
    "DU\t"        ""
    "DU\tfoo.clj" "foo.clj"
    "foo.cljDU\t" "foo.cljDU\t"

    ;; `R` is replaced including optional previous path
    "R001\tfoo.clj"          "foo.clj"
    "R100\tfoo.clj\tbar.clj" "bar.clj"
    "R087\tfoo.clj\tbar.clj" "bar.clj"
    "foo.cljR100\tbar.clj"   "foo.cljR100\tbar.clj"

    ;; `Ñ` is not replaced, because `git-diff` does not emit that

    "Ñ\t"        "Ñ\t"
    "Ñ\tfoo.clj" "Ñ\tfoo.clj"
    "foo.cljÑ\t" "foo.cljÑ\t"))
