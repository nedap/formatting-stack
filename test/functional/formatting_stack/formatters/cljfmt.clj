(ns functional.formatting-stack.formatters.cljfmt
  (:require
   [clojure.test :refer [are deftest]]
   [formatting-stack.formatters.cljfmt :as sut]
   [formatting-stack.protocols.linter :as linter]
   [matcher-combinators.test :refer [match?]]))

(deftest lint!
  (let [linter (sut/new {})]
    (are [filename expected] (match? expected
                                     (linter/lint! linter [filename]))
      "test-resources/valid_syntax.clj"
      []

      "test-resources/invalid_syntax.clj"
      [{:source :formatting-stack/report-processing-error
        :level :exception
        :filename "test-resources/invalid_syntax.clj"}]

      "test-resources/wrong_indent.clj"
      [{:source :cljfmt/indent
        :msg "Indentation or whitespace is off on line 4-5"
        :line 4
        :column 0
        :filename "test-resources/wrong_indent.clj"}
       {:source :cljfmt/indent
        :msg "Indentation or whitespace is off on line 7"
        :line 7
        :column 0
        :filename "test-resources/wrong_indent.clj"}])))
