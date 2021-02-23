(ns unit.formatting-stack.reporters.pretty-printer
  (:require
   [clojure.test :refer [are deftest is testing]]
   [formatting-stack.reporters.pretty-printer :as sut]))

(deftest print-warnings
  (are [desc input expected] (testing [desc input]
                               (is (= expected
                                      (with-out-str
                                        (sut/print-warnings {:colorize? false
                                                             :print-diff? true
                                                             :max-msg-length 20}
                                                            input))))
                               true)
    "Empty"
    []
    ""

    "Basic"
    [{:filename "filename", :msg "message", :source ::source, :level :warning, :line 0 :column 0}]
    "filename\n  :unit.formatting-stack.reporters.pretty-printer/source\n    0:0 message\n\n"

    "Sorts by `:line`"
    [{:filename "filename", :msg "message", :source ::source, :level :warning, :line 0 :column 0}
     {:filename "filename", :msg "message", :source ::source, :level :warning, :line 2 :column 2}
     {:filename "filename", :msg "message", :source ::source, :level :warning, :line 1 :column 1}]
    "filename\n  :unit.formatting-stack.reporters.pretty-printer/source\n    0:0 message\n    1:1 message\n    2:2 message\n\n"

    "Groups by `:source`"
    [{:filename "filename", :msg "message", :source ::source-A, :level :warning, :line 0 :column 0}
     {:filename "filename", :msg "message", :source ::source-B, :level :warning, :line 2 :column 2}
     {:filename "filename", :msg "message", :source ::source-A, :level :warning, :line 1 :column 1}]
    "filename\n  :unit.formatting-stack.reporters.pretty-printer/source-A\n    0:0 message\n    1:1 message\n  :unit.formatting-stack.reporters.pretty-printer/source-B\n    2:2 message\n\n"

    "Can print a given `:warning-details-url`, once at most per `:source` group"
    [{:filename "filename", :msg "message", :source ::source-A, :level :warning, :line 0 :column 0}
     {:filename "filename", :msg "message", :source ::source-A, :level :warning, :line 1 :column 1 :warning-details-url "http://example.test/foo"}]
    "filename\n  :unit.formatting-stack.reporters.pretty-printer/source-A\n    See: http://example.test/foo\n    0:0 message\n    1:1 message\n\n"

    "Can print `:msg-extra-data` (at the correct indentation level)"
    [{:filename "filename", :msg "message", :source ::source, :level :warning, :line 0 :column 0, :msg-extra-data ["Foo" "Bar"]}]
    "filename\n  :unit.formatting-stack.reporters.pretty-printer/source\n    0:0 message\n        Foo\n        Bar\n\n"

    "Can print missing `:column` and `:line`"
    [{:filename "filename", :msg "message", :source ::source, :level :warning}]
    "filename\n  :unit.formatting-stack.reporters.pretty-printer/source\n    ?:? message\n\n"

    "Can print `:diff`"
    [{:filename "filename", :msg "message", :source ::source, :level :warning, :line 0 :column 0 :diff (slurp "test-resources/diffs/files/1.diff")}]
    "filename\n  :unit.formatting-stack.reporters.pretty-printer/source\n    0:0 message\n--- a/mocked/absolute/path/test-resources/diffs/files/1.txt\n+++ b/mocked/absolute/path/test-resources/diffs/files/1.txt\n@@ -1,1 +1,1 @@\n-\n+Hello World!\n\n"))

(deftest print-summary
  (are [input expected] (= expected
                           (with-out-str
                             (sut/print-summary {:colorize? false
                                                 :summary? true}
                                                input)))
    (repeat 5 {:level :warning})
    "5 warnings found\n"

    (repeat 4 {:level :error})
    "4 errors found\n"

    (repeat 3 {:level :exception})
    "3 exceptions occurred\n"

    (concat (repeat 5 {:level :warning})
            (repeat 4 {:level :error})
            (repeat 3 {:level :exception}))
    "4 errors found\n3 exceptions occurred\n5 warnings found\n"))
