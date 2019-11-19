(ns functional.formatting-stack.formatters.clean-ns.impl
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer :all]
   [formatting-stack.formatters.clean-ns.impl :as sut]))

(deftest has-duplicate-requires?
  (are [input assertion] (let [file (io/file "test" "functional" "formatting_stack" "formatters" "clean_ns" input)
                               _ (assert (-> file .exists))
                               filename (-> file .getAbsolutePath)
                               result (sut/has-duplicate-requires? filename)]
                           (case assertion
                             :has-duplicates result
                             :does-not-have-duplicates (not result)))
    "has_duplicates.clj"           :has-duplicates
    "has_duplicates_2.clj"         :has-duplicates
    "has_duplicates_3.clj"         :has-duplicates
    "impl.clj"                     :does-not-have-duplicates
    "does_not_have_duplicates.clj" :does-not-have-duplicates))

(deftest ns-form-of
  (are [input expected] (= expected
                           (sut/ns-form-of input))
    "test-resources/sample_clj_ns.clj"   '(ns sample-clj-ns
                                            (:require [foo.bar.baz :as baz])
                                            (:import (java.util UUID)))
    "test-resources/sample_cljc_ns.cljc" '(ns sample-cljc-ns
                                            (:require [foo.bar.baz :as baz-clj])
                                            (:import (java.util UUID)))
    "test-resources/sample_cljs_ns.cljs" '(ns sample-cljs-ns
                                            (:require [foo.bar.baz :as baz])
                                            (:require-macros [sample-cljs-ns :refer [the-macro]]))))
