(ns functional.formatting-stack.formatters.clean-ns
  (:require
   [clojure.test :refer :all]
   [formatting-stack.formatters.clean-ns :as sut]
   [formatting-stack.formatters.clean-ns.impl :as impl :refer [ns-form-of]]
   [formatting-stack.formatters.how-to-ns]
   [formatting-stack.util.ns :as util.ns]
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

(def should-not-be-cleaned-3-f "test/functional/formatting_stack/formatters/clean_ns/should_not_be_cleaned_3.clj")
(def should-not-be-cleaned-3 (ns-form-of should-not-be-cleaned-3-f))

(def should-not-be-cleaned-4-f "test/functional/formatting_stack/formatters/clean_ns/should_not_be_cleaned_4.clj")
(def should-not-be-cleaned-4 (ns-form-of should-not-be-cleaned-4-f))

(def should-not-be-cleaned-5-f "test/functional/formatting_stack/formatters/clean_ns/should_not_be_cleaned_5.clj")
(def should-not-be-cleaned-5 (ns-form-of should-not-be-cleaned-5-f))

(assert should-be-cleaned)

(assert should-not-be-partially-cleaned)

(assert should-not-be-cleaned)

(assert should-not-be-cleaned-2)

(assert should-not-be-cleaned-3)

(assert should-not-be-cleaned-4)

(assert should-not-be-cleaned-5)

(deftest used-namespace-names
  (is (not (seq (impl/used-namespace-names should-be-cleaned-f #{}))))
  (is (seq (impl/used-namespace-names should-not-be-partially-cleaned-f #{})))
  (is (seq (impl/used-namespace-names should-not-be-cleaned-f #{})))
  (is (seq (impl/used-namespace-names should-not-be-cleaned-2-f #{}))))

(deftest clean-ns-form
  (are [op filename libspec-whitelist namespaces-that-should-never-cleaned]
       (let [cleaner (sut/make-cleaner formatting-stack.formatters.how-to-ns/default-how-to-ns-opts
                                       sut/default-nrepl-opts
                                       namespaces-that-should-never-cleaned
                                       libspec-whitelist
                                       filename)
             v (util.ns/replaceable-ns-form filename cleaner formatting-stack.formatters.how-to-ns/default-how-to-ns-opts)]
         (op v))
    some? should-be-cleaned-f               sut/default-libspec-whitelist #{}
    nil?  should-be-cleaned-f               sut/default-libspec-whitelist #{'functional.formatting-stack.formatters.clean-ns.should-be-cleaned}
    some? "dev/user.clj"                    sut/default-libspec-whitelist #{}
    nil?  "dev/user.clj"                    sut/default-libspec-whitelist sut/default-namespaces-that-should-never-cleaned
    nil?  should-not-be-partially-cleaned-f sut/default-libspec-whitelist #{}
    nil?  should-not-be-cleaned-f           sut/default-libspec-whitelist #{}
    nil?  should-not-be-cleaned-2-f         sut/default-libspec-whitelist #{}
    nil?  should-not-be-cleaned-3-f         sut/default-libspec-whitelist #{}
    nil?  should-not-be-cleaned-4-f         sut/default-libspec-whitelist #{}
    nil?  should-not-be-cleaned-5-f         sut/default-libspec-whitelist #{}
    some? should-not-be-cleaned-3-f         #{}                           #{}
    some? should-not-be-cleaned-4-f         #{}                           #{}
    some? should-not-be-cleaned-5-f         #{}                           #{}))
