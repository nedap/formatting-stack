(ns unit.formatting-stack.util.diff
  (:require
   [clojure.test :refer [are deftest is testing]]
   [formatting-stack.util.diff :as sut]))

(deftest diff->line-numbers
  (are [description filename expected] (testing description
                                         (is (= expected
                                                (sut/diff->line-numbers (slurp filename))))
                                         true)

    "only additions yields empty result"
    "test-resources/diffs/1.patch"
    []

    "3 sections for one file"
    "test-resources/diffs/2.patch"
    [{:begin    14
      :end      14
      :filename "src/formatting_stack/formatters/trivial_ns_duplicates.clj"}
     {:begin    144
      :end      144
      :filename "src/formatting_stack/formatters/trivial_ns_duplicates.clj"}
     {:begin    146
      :end      146
      :filename "src/formatting_stack/formatters/trivial_ns_duplicates.clj"}
     {:begin    154
      :end      154
      :filename "src/formatting_stack/formatters/trivial_ns_duplicates.clj"}]

    "multiple changed lines in one section"
    "test-resources/diffs/3.patch"
    [{:begin    12
      :end      12
      :filename "src/formatting_stack/formatters/no_extra_blank_lines.clj"}
     {:begin    30
      :end      33
      :filename "src/formatting_stack/formatters/no_extra_blank_lines.clj"}
     {:begin    41
      :end      41
      :filename "src/formatting_stack/protocols/spec.clj"}]

    "moved code"
    "test-resources/diffs/4.patch"
    [{:begin    5
      :end      5
      :filename "test/unit/formatting_stack/strategies.clj"}]))
