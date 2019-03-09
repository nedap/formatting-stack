(ns unit.formatting-stack.formatters.cljfmt.impl
  (:require
   [clojure.test :refer :all]
   [formatting-stack.formatters.cljfmt.impl :as sut]))

(deftest setup-cljfmt-indents!

  (testing "No nil rules are produced"
    (is (every? identity (-> (sut/setup-cljfmt-indents! {})
                             vals))))

  (testing "`:defn` resolves as a known-good rule"
    (let [key (gensym)
          rule (get (sut/setup-cljfmt-indents! {key :defn})
                    key)]
      (is (= [[:inner 0] rule]))))

  (testing "Nonsensical rules are omitted"
    (let [key (gensym)
          rule (get (sut/setup-cljfmt-indents! {key :sdlkfjdslfj})
                    key
                    ::not-found)]
      (is (= ::not-found rule)))))
