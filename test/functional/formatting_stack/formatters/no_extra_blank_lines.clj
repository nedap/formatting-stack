(ns functional.formatting-stack.formatters.no-extra-blank-lines
  (:require
   [clojure.test :refer [are deftest]]
   [formatting-stack.formatters.no-extra-blank-lines :as sut]
   [formatting-stack.protocols.linter :as linter]
   [matcher-combinators.test :refer [match?]]))

(deftest lint!
  (let [linter (sut/new)]
    (are [filename expected] (match? expected
                                     (linter/lint! linter [filename]))
      "test-resources/valid_syntax.clj"
      []

      "test-resources/invalid_syntax.clj"
      []

      "test-resources/extra_newlines_warning.clj"
      [{:source :formatting-stack/no-extra-blank-lines
        :msg "File has two or more consecutive blank lines"
        :line 4
        :column 1
        :filename "test-resources/extra_newlines_warning.clj"}])))
