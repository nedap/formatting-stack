(ns functional.formatting-stack.formatters.newlines
  (:require
   [clojure.test :refer [are deftest]]
   [formatting-stack.formatters.newlines :as sut]
   [formatting-stack.protocols.linter :as linter]
   [matcher-combinators.test :refer [match?]]))

(deftest lint!
  (let [linter (sut/new {})]
    (are [filename expected] (match? expected
                                     (linter/lint! linter [filename]))
      "test-resources/valid_syntax.clj"
      []

      "test-resources/invalid_syntax.clj"
      []

      "test-resources/extra_newlines_warning.clj"
      [{:source :formatting-stack/newlines
        :msg "File should end in 1 newlines"
        :line 7
        :column 1
        :filename "test-resources/extra_newlines_warning.clj"}])))
