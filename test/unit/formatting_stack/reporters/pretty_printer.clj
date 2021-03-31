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
    "filename
  :unit.formatting-stack.reporters.pretty-printer/source
    0:0 message\n\n"

    "Sorts by `:line`"
    [{:filename "filename", :msg "message", :source ::source, :level :warning, :line 0 :column 0}
     {:filename "filename", :msg "message", :source ::source, :level :warning, :line 2 :column 2}
     {:filename "filename", :msg "message", :source ::source, :level :warning, :line 1 :column 1}]
    "filename
  :unit.formatting-stack.reporters.pretty-printer/source
    0:0 message
    1:1 message
    2:2 message\n\n"

    "Groups by `:source`"
    [{:filename "filename", :msg "message", :source ::source-A, :level :warning, :line 0 :column 0}
     {:filename "filename", :msg "message", :source ::source-B, :level :warning, :line 2 :column 2}
     {:filename "filename", :msg "message", :source ::source-A, :level :warning, :line 1 :column 1}]
    "filename
  :unit.formatting-stack.reporters.pretty-printer/source-A
    0:0 message
    1:1 message
  :unit.formatting-stack.reporters.pretty-printer/source-B
    2:2 message\n\n"

    "Can print a given `:warning-details-url`, once at most per `:source` group"
    [{:filename "filename", :msg "message", :source ::source-A, :level :warning, :line 0 :column 0}
     {:filename "filename", :msg "message", :source ::source-A, :level :warning, :line 1 :column 1 :warning-details-url "http://example.test/foo"}]
    "filename
  :unit.formatting-stack.reporters.pretty-printer/source-A
    See: http://example.test/foo
    0:0 message
    1:1 message\n\n"

    "Can print `:msg-extra-data` (at the correct indentation level)"
    [{:filename "filename", :msg "message", :source ::source, :level :warning, :line 0 :column 0, :msg-extra-data ["Foo" "Bar"]}]
    "filename
  :unit.formatting-stack.reporters.pretty-printer/source
    0:0 message
        Foo
        Bar\n\n"

    "Can print missing `:column` and `:line`"
    [{:filename "filename", :msg "message", :source ::source, :level :warning}]
    "filename
  :unit.formatting-stack.reporters.pretty-printer/source
    ?:? message\n\n"

    "Can print `:diff`"
    [{:filename "filename", :msg "message", :source ::source, :level :warning, :line 0 :column 0 :diff (slurp "test-resources/diffs/files/1.diff")}]
    "filename
  :unit.formatting-stack.reporters.pretty-printer/source
    0:0 message
--- a/mocked/absolute/path/test-resources/diffs/files/1.txt
+++ b/mocked/absolute/path/test-resources/diffs/files/1.txt
@@ -1,1 +1,1 @@
-
+Hello World!\n\n"))

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
    "4 errors found
3 exceptions occurred
5 warnings found\n"))
