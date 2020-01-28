(ns unit.formatting-stack.linters.one-resource-per-ns
  (:require
   [clojure.test :refer :all]
   [formatting-stack.linters.one-resource-per-ns :as sut]
   [formatting-stack.test-helpers :as test-helpers]))

(deftest ns-decl->resource-path
  (are [input expected] (testing input
                          (is (= expected
                                 (sut/ns-decl->resource-path input ".clj")))
                          true)
    '(ns unit)                                              "unit.clj"
    '(ns unit.formatting-stack.linters.one-resource-per-ns) "unit/formatting_stack/linters/one_resource_per_ns.clj"
    '(ns foo!?)                                             "foo_BANG__QMARK_.clj"))

(deftest resource-path->filenames
  (are [input] (testing input
                 (is (= (-> "test/" (str input) test-helpers/filename-as-resource vector)
                        (sut/resource-path->filenames input)))
                 true)
    "unit/formatting_stack/linters/one_resource_per_ns.clj"
    "unit/formatting_stack/linters/ns_aliases.clj"))
