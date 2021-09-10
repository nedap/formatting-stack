(ns unit.formatting-stack.reporters.pretty-line-printer
  (:require
   [clojure.test :refer [are deftest is testing]]
   [formatting-stack.reporters.pretty-line-printer :as sut]))

(deftest print-warnings
  (are [desc input expected] (testing [desc input]
                               (is (= expected
                                      (with-out-str
                                        (sut/print-warnings {:colorize? false
                                                             :max-msg-length 20}
                                                            input))))
                               true)
    "Empty"
    []
    ""

    "Basic"
    [{:filename "filename", :msg "message", :source ::source, :level :warning, :line 0 :column 0}]
    "filename
 ⚠️   0:0   message                :unit.formatting-stack.reporters.pretty-line-printer/source\n"

    "Truncating message"
    [{:filename "filename", :source ::source, :level :warning, :line 0 :column 0,
      :msg "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et"}]
    "filename
 ⚠️   0:0   Lorem ipsum dolor si   :unit.formatting-stack.reporters.pretty-line-printer/source\n"

    "Sorts by `:line`"
    [{:filename "filename", :msg "message", :source ::source, :level :warning, :line 0 :column 0}
     {:filename "filename", :msg "message", :source ::source, :level :warning, :line 2 :column 2}
     {:filename "filename", :msg "message", :source ::source, :level :warning, :line 1 :column 1}]
    "filename
 ⚠️   0:0   message                :unit.formatting-stack.reporters.pretty-line-printer/source
 ⚠️   1:1   message                :unit.formatting-stack.reporters.pretty-line-printer/source
 ⚠️   2:2   message                :unit.formatting-stack.reporters.pretty-line-printer/source\n"

    "Groups by `:source`"
    [{:filename "filename", :msg "message", :source ::source-A, :level :warning, :line 0 :column 0}
     {:filename "filename", :msg "message", :source ::source-B, :level :warning, :line 2 :column 2}
     {:filename "filename", :msg "message", :source ::source-A, :level :warning, :line 1 :column 1}]
    "filename
 ⚠️   0:0   message                :unit.formatting-stack.reporters.pretty-line-printer/source-A
 ⚠️   1:1   message                :unit.formatting-stack.reporters.pretty-line-printer/source-A
 ⚠️   2:2   message                :unit.formatting-stack.reporters.pretty-line-printer/source-B\n"

    "Can print `:msg-extra-data` (at the correct indentation level)"
    [{:filename "filename", :msg "message", :source ::source, :level :warning, :line 0 :column 0, :msg-extra-data ["Foo" "Bar"]}]
    "filename
 ⚠️   0:0   message                :unit.formatting-stack.reporters.pretty-line-printer/source
            Foo
            Bar\n"

    "Can print missing `:column` and `:line`"
    [{:filename "filename", :msg "message", :source ::source, :level :warning}]
    "filename
 ⚠️         message                :unit.formatting-stack.reporters.pretty-line-printer/source\n"))

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
