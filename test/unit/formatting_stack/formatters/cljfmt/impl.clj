(ns unit.formatting-stack.formatters.cljfmt.impl
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer [are deftest is testing]]
   [formatting-stack.formatters.cljfmt.impl :as sut]
   [unit.formatting-stack.formatters.cljfmt.impl.sample-data :refer [foo]])
  (:import
   (java.io File)))

(def ^File this-file
  "A file that contains a 'foo refer."
  (io/file "test" "unit" "formatting_stack" "formatters" "cljfmt" "impl.clj"))

(def ^File related-file
  "A file that contains a 'foo def."
  (io/file "test" "unit" "formatting_stack" "formatters" "cljfmt" "impl" "sample_data.clj"))

(def ^File unrelated-file
  "A file that contains no 'foo def or refer."
  (io/file "test" "unit" "formatting_stack" "strategies.clj"))

(assert (-> this-file .exists))

(assert (-> related-file .exists))

(assert (-> unrelated-file .exists))

(deftest cljfmt-indents-for

  (testing "No nil rules are produced"
    (is (every? identity (-> (sut/cljfmt-indents-for nil {})
                             vals))))

  (testing "`:defn` resolves as a known-good rule"
    (let [key (gensym)
          rule (get (sut/cljfmt-indents-for nil {key {:style/indent :defn}})
                    key)]
      (is (= [[:inner 0]]
             rule))))

  (testing "Nonsensical rules are omitted"
    (let [key (gensym)
          rule (get (sut/cljfmt-indents-for nil {key {:style/indent :sdlkfjdslfj}})
                    key
                    ::not-found)]
      (is (= ::not-found
             rule))))

  (testing "`def`s and `:refer`s are understood"
    (are [file e] (let [{u 'foo
                         q 'unit.formatting-stack.formatters.cljfmt.impl.sample-data/foo} (-> file
                                                                                              str
                                                                                              (sut/cljfmt-indents-for {}))]
                    (case e
                      :unqualified-too
                      (do
                        (is (= [[:block 0]]
                               u))
                        (is (= [[:block 0]]
                               q)))

                      :qualified-only
                      (do
                        (is (nil? u))
                        (is (= q
                               [[:block 0]]))))

                    true)

      this-file      :unqualified-too
      related-file   :unqualified-too
      unrelated-file :qualified-only))

  (testing "Rules can override clojure.core's"
    (are [file e] (let [{u 'do
                         q 'unit.formatting-stack.formatters.cljfmt.impl.sample-data/do} (-> file
                                                                                             str
                                                                                             (sut/cljfmt-indents-for {}))]

                    (case e
                      :unqualified-too
                      (do
                        (is (= [[:block 7]]
                               u))
                        (is (= [[:block 7]]
                               q)))

                      :qualified-only
                      (do
                        (is (= [[:block 0]]
                               u))
                        (is (= [[:block 7]]
                               q))))

                    true)

      this-file    :qualified-only
      related-file :unqualified-too)))
