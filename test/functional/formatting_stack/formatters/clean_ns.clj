(ns functional.formatting-stack.formatters.clean-ns
  (:require
   [clojure.test :refer :all]
   [formatting-stack.formatters.clean-ns.impl :as impl :refer [ns-form-of]]
   [formatting-stack.formatters.how-to-ns]
   [functional.formatting-stack.formatters.clean-ns.should-be-cleaned]
   [functional.formatting-stack.formatters.clean-ns.should-not-be-cleaned]
   [functional.formatting-stack.formatters.clean-ns.should-not-be-cleaned-2]
   [functional.formatting-stack.formatters.clean-ns.should-not-be-partially-cleaned]))

(def should-be-cleaned-f "test/functional/formatting_stack/formatters/clean_ns/should_be_cleaned.clj")
(def should-be-cleaned (ns-form-of should-be-cleaned-f))

(def should-not-be-partially-cleaned-f "test/functional/formatting_stack/formatters/clean_ns/should_not_be_partially_cleaned.clj")
(def should-not-be-partially-cleaned (ns-form-of should-not-be-partially-cleaned-f))

(def should-not-be-cleaned-f "test/functional/formatting_stack/formatters/clean_ns/should_not_be_cleaned.clj")
(def should-not-be-cleaned (ns-form-of should-not-be-cleaned-f))

(def should-not-be-cleaned-2-f "test/functional/formatting_stack/formatters/clean_ns/should_not_be_cleaned_2.clj")
(def should-not-be-cleaned-2 (ns-form-of should-not-be-cleaned-2-f))

(assert should-be-cleaned)

(assert should-not-be-partially-cleaned)

(assert should-not-be-cleaned)

(assert should-not-be-cleaned-2)

(deftest used-namespace-names
  (is (not (seq (impl/used-namespace-names should-be-cleaned-f))))
  (is (seq (impl/used-namespace-names should-not-be-partially-cleaned-f)))
  (is (seq (impl/used-namespace-names should-not-be-cleaned-f)))
  (is (seq (impl/used-namespace-names should-not-be-cleaned-2-f))))

(deftest clean-ns-form
  (are [op filename ns-form] (op (read-string (impl/clean-ns-form formatting-stack.formatters.how-to-ns/default-how-to-ns-opts
                                                                  filename
                                                                  ns-form))
                                 ns-form)
    not= should-be-cleaned-f               should-be-cleaned
    =    should-not-be-partially-cleaned-f should-not-be-partially-cleaned
    =    should-not-be-cleaned-f           should-not-be-cleaned
    =    should-not-be-cleaned-2-f         should-not-be-cleaned-2))
