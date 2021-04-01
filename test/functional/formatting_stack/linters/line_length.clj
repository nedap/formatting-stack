(ns functional.formatting-stack.linters.line-length
  (:require
   [clojure.test :refer [are deftest]]
   [formatting-stack.linters.line-length :as sut]
   [formatting-stack.protocols.linter :as linter]
   [matcher-combinators.test :refer [match?]]))

(deftest lint!
  (let [linter (sut/new {:max-line-length 24
                         :merge-threshold 5})]
    (are [filename expected] (match? expected
                                     (linter/lint! linter [filename]))
      "test-resources/valid_syntax.clj"
      []

      "test-resources/invalid_syntax.clj"
      []

      "test-resources/sample_clj_ns.clj"
      [{:source   :formatting-stack/line-length
        :line     3
        :column   25
        :msg      "Line exceeding 24 columns."
        :filename "test-resources/sample_clj_ns.clj"}]

      "test-resources/linelength_sample.clj"
      [{:filename "test-resources/linelength_sample.clj",
        :source :formatting-stack/line-length,
        :level :warning,
        :line 3,
        :column 54,
        :msg "Line exceeding 24 columns."}
       {:source   :formatting-stack/line-length
        :line     13
        :column   54
        :msg      "Line exceeding 24 columns (spanning 2 lines)."
        :filename "test-resources/linelength_sample.clj"}
       {:filename "test-resources/linelength_sample.clj",
        :source :formatting-stack/line-length,
        :level :warning,
        :line 22,
        :column 54,
        :msg "Line exceeding 24 columns (spanning 3 lines)."}])))
