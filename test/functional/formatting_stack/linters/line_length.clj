(ns functional.formatting-stack.linters.line-length
  (:require
   [clojure.test :refer [are deftest]]
   [formatting-stack.linters.line-length :as sut]
   [formatting-stack.protocols.linter :as linter]
   [matcher-combinators.test :refer [match?]]))

(deftest lint!
  (let [linter (sut/new {:max-line-length 22})]
    (are [filename expected] (match? expected
                                     (linter/lint! linter [filename]))
      "test-resources/invalid_syntax.clj"
      []

      "test-resources/sample_clj_ns.clj"
      [{:source   :formatting-stack/line-length
        :line     3
        :column   23
        :msg      "Line exceeding 22 columns"
        :filename "test-resources/sample_clj_ns.clj"}])))
