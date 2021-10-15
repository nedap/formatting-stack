(ns unit.formatting-stack.util
  (:require
   [clojure.string :as str]
   [clojure.test :refer :all]
   [formatting-stack.util :as sut]
   [formatting-stack.util.diff :as diff]
   [matcher-combinators.test :refer [match?]]))

(deftest read-ns-decl
  (are [input expected] (= expected
                           (sut/read-ns-decl input))
    "test-resources/sample_clj_ns.clj"   '(ns sample-clj-ns
                                            (:require [foo.bar.baz :as baz])
                                            (:import (java.util UUID)))
    "test-resources/sample_cljc_ns.cljc" '(ns sample-cljc-ns
                                            (:require [foo.bar.baz :as baz-clj])
                                            (:import (java.util UUID)))
    "test-resources/sample_cljs_ns.cljs" '(ns sample-cljs-ns
                                            (:require [foo.bar.baz :as baz])
                                            (:require-macros [sample-cljs-ns :refer [the-macro]]))))

(deftest process-in-parallel!
  (testing "error reporting"
    (are [f xs expected] (match? expected
                                 (sut/process-in-parallel! f xs))
      (fn [_] (throw (ex-info "expected" {})))
      ["made_up_name.clj"]
      [{:exception #(= "expected" (ex-message %))
        :level     :exception
        :filename  "made_up_name.clj"
        :msg       #(str/starts-with? % "Encountered an exception while running")}]

      (fn [_] (assert false "expected"))
      ["made_up_name.clj"]
      [{:exception #(= "Assert failed: expected\nfalse" (ex-message %))
        :level     :exception
        :filename  "made_up_name.clj"
        :msg       #(str/starts-with? % "Encountered an exception while running")}]

      (fn [_] (diff/diff->line-numbers (slurp "test-resources/diffs/incorrect.diff")))
      ["made_up_name.clj"]
      [{:exception #(= "A FROM_FILE line ('---') must be directly followed by a TO_FILE line ('+++')!" (ex-message %))
        :level     :exception
        :filename  "made_up_name.clj"
        :msg       #(str/starts-with? % "Encountered an exception while running")}])))

(deftest partition-between
  (are [pred input expected] (= expected
                                (sut/partition-between pred input))
    identity
    ()
    ()

    >
    '(1 2 3)
    '((1 2 3))

    <
    '(1 2 3)
    '((1) (2) (3))

    #(< 2 (- %2 %1))
    '(1 2 3 8 9 10)
    '((1 2 3) (8 9 10))))

(deftest unlimited-pr-str
  (let [input [1 2 [3 4 [5 6]]]
        expected "[1 2 [3 4 [5 6]]]"]
    ;; override user settings
    (binding [*print-length* nil
              *print-level* nil]
      (is (= expected (sut/unlimited-pr-str input)))

      (testing "not affected by *print-length*"
        (binding [*print-length* 1]
          (is (= expected (sut/unlimited-pr-str input)))))

      (testing "not affected by *print-level*"
        (binding [*print-level* 1]
          (is (= expected (sut/unlimited-pr-str input))))))))
