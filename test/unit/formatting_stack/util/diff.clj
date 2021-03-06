(ns unit.formatting-stack.util.diff
  (:require
   [clojure.test :refer [are deftest is testing use-fixtures]]
   [formatting-stack.test-helpers :refer [with-mocked-diff-path]]
   [formatting-stack.util.diff :as sut]))

(use-fixtures :once with-mocked-diff-path)

(deftest diff->line-numbers
  (are [description filename expected] (testing description
                                         (is (= expected
                                                (sut/diff->line-numbers (slurp filename))))
                                         true)

    "Additions do not report a line number"
    "test-resources/diffs/1.diff"
    []

    "Multiple sections are reported individually"
    "test-resources/diffs/2.diff"
    [{:start    14
      :end      14
      :filename "src/formatting_stack/formatters/trivial_ns_duplicates.clj"}
     {:start    144
      :end      144
      :filename "src/formatting_stack/formatters/trivial_ns_duplicates.clj"}
     {:start    146
      :end      146
      :filename "src/formatting_stack/formatters/trivial_ns_duplicates.clj"}
     {:start    154
      :end      154
      :filename "src/formatting_stack/formatters/trivial_ns_duplicates.clj"}]

    "consecutive removed lines are grouped"
    "test-resources/diffs/3.diff"
    [{:start    12
      :end      12
      :filename "src/formatting_stack/formatters/no_extra_blank_lines.clj"}
     {:start    30
      :end      33
      :filename "src/formatting_stack/formatters/no_extra_blank_lines.clj"}
     {:start    41
      :end      41
      :filename "src/formatting_stack/protocols/spec.clj"}]

    "Moving code reports on the removed line"
    "test-resources/diffs/4.diff"
    [{:start    5
      :end      5
      :filename "test/unit/formatting_stack/strategies.clj"}]

    "Renaming does not report anything"
    "test-resources/diffs/5.diff"
    []

    "Deleting reports entire file"
    "test-resources/diffs/6.diff"
    [{:end      82
      :start    1
      :filename "/dev/null"}]

    "Adding a new file does not report anything"
    "test-resources/diffs/7.diff"
    []

    "Can handle `\\ No newline at end of file`"
    "test-resources/diffs/8.diff"
    [])

  (testing "exceptions are passed through"
    (is (thrown? IllegalStateException
                 (sut/diff->line-numbers (slurp "test-resources/diffs/incorrect.diff"))))))

(deftest unified-diff
  (are [description filename revised-filename expected] (testing description
                                                          (is (= (slurp expected)
                                                                 (sut/unified-diff filename
                                                                                   (slurp filename)
                                                                                   (slurp revised-filename))))
                                                          true)

    "Adding to an empty file"
    "test-resources/diffs/files/1.txt"
    "test-resources/diffs/files/1_revised.txt"
    "test-resources/diffs/files/1.diff"

    "Removing all contents"
    "test-resources/diffs/files/1_revised.txt"
    "test-resources/diffs/files/1.txt"
    "test-resources/diffs/files/1_reversed.diff"

    "Removing newlines"
    "test-resources/diffs/files/2.txt"
    "test-resources/diffs/files/2_revised.txt"
    "test-resources/diffs/files/2.diff"

    "Adding newlines"
    "test-resources/diffs/files/2_revised.txt"
    "test-resources/diffs/files/2.txt"
    "test-resources/diffs/files/2_reversed.diff"))
