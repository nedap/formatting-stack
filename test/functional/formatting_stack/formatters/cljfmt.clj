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
      "test-resources/invalid_syntax.clj"
      [{:source :formatting-stack/report-processing-error
        :level :exception
        :filename "test-resources/invalid_syntax.clj"}]

      "test-resources/wrong_indent.clj"
      [{:source :cljfmt/indent
        :msg "Indentation is wrong between 1-7"
        :line 1
        :column 1
        :filename "test-resources/wrong_indent.clj"}])))
