(ns unit.formatting-stack.util
  (:require
   [clojure.test :refer :all]
   [formatting-stack.util :as sut]))

(deftest read-ns-decl
  (are [input expected] (= expected
                           (sut/read-ns-decl input))
    "test-resources/sample_clj_ns.clj"   '(ns sample-clj-ns
                                            (:require [foo.bar.baz :as baz])
                                            (:import (java.util UUID)))
    "test-resources/sample_cljc_ns.cljc" '(ns sample-cljc-ns
                                            (:require [foo.bar.baz :as baz-clj])
                                            (:import (java.util UUID)))
    "test-resources/sample_cljs_ns.cljs" '(ns sample-cljs-ns
                                            (:require [foo.bar.baz :as baz])
                                            (:require-macros [sample-cljs-ns :refer [the-macro]]))))
(deftest diff->line-numbers
  (are [description filename expected] (testing description
                                         (is (= expected
                                                (sut/diff->line-numbers (slurp filename))))
                                         true)
    "only additions yields empty result"
    "test-resources/diffs/1.patch"
    ()

    "patch with 3 sections for one file"
    "test-resources/diffs/2.patch"
    [{:begin    14
      :end      14
      :filename "b/src/formatting_stack/formatters/trivial_ns_duplicates.clj"}
     {:begin    144
      :end      144
      :filename "b/src/formatting_stack/formatters/trivial_ns_duplicates.clj"}
     {:begin    146
      :end      146
      :filename "b/src/formatting_stack/formatters/trivial_ns_duplicates.clj"}
     {:begin    154
      :end      154
      :filename "b/src/formatting_stack/formatters/trivial_ns_duplicates.clj"}]

    "patch with multiple changed lines in one section"
    "test-resources/diffs/3.patch"
    [{:begin    12
      :end      12
      :filename "b/src/formatting_stack/formatters/no_extra_blank_lines.clj"}
     {:begin    30
      :end      33
      :filename "b/src/formatting_stack/formatters/no_extra_blank_lines.clj"}
     {:begin    41
      :end      41
      :filename "b/src/formatting_stack/protocols/spec.clj"}]

    "patch with only additions"
    "test-resources/diffs/4.patch"
    []))
