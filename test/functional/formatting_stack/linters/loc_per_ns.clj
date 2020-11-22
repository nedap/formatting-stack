(ns functional.formatting-stack.linters.loc-per-ns
  (:require
   [clojure.test :refer [are deftest]]
   [formatting-stack.linters.loc-per-ns :as sut]
   [formatting-stack.protocols.linter :as linter]
   [matcher-combinators.test :refer [match?]]))

(deftest lint!
  (let [linter (sut/new {:max-lines-per-ns 4})]
    (are [filename expected] (match? expected
                                     (linter/lint! linter [filename]))
      "test-resources/valid_syntax.clj"
      []

      "test-resources/invalid_syntax.clj"
      []

      "test-resources/sample_clj_ns.clj"
      [{:source   :formatting-stack/loc-per-ns
        :line     5
        :column   0
        :msg      "Longer than 4 LOC."
        :filename "test-resources/sample_clj_ns.clj"}])))
