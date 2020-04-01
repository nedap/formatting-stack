(ns unit.formatting-stack.impl.overrides
  (:require
   [clojure.test :refer [are deftest is testing]]
   [formatting-stack.impl.overrides :as sut]))

(deftest removing-deep-merge
  (are [a b expected] (testing (pr-str [a b])
                        (is (= expected
                               (sut/removing-deep-merge a b)))
                        true)
    {}               nil                          nil
    nil              {}                           {}
    {}               {:a 2}                       {:a 2}
    {:a 2}           {}                           {:a 2}
    {:a 2}           {:b 3}                       {:a 2 :b 3}
    {:a 2}           {:a nil}                     {}
    {:a {:b {:c 1}}} {:a {:b {:c 2}}}             {:a {:b {:c 2}}}
    {:a {:b {:c 1}}} {:a {:b {:c 2 :d 3} :e 4}}   {:a {:b {:c 2 :d 3} :e 4}}
    {:a {:b {:c 1}}} {:a {:b {:c nil}}}           {:a {:b {}}}
    {:a {:b {:c 1}}} {:a {:b {:c nil :d {:e 2}}}} {:a {:b {:d {:e 2}}}}))

(deftest apply-overrides
  (let [sample-members [{:id :a/id :age 42 :bar {:baz 42}}]
        reified-member (reify)]
    (are [desc members overrides expected] (testing (pr-str [desc members overrides])
                                             (is (= expected
                                                    (try
                                                      (sut/apply-overrides members overrides)
                                                      (catch AssertionError _
                                                        ::EXCEPTION))))
                                             true)

      "Overrides are deep-merged"
      sample-members
      {:a/id {:foo {3 4} :bar {:m 222}}}
      [{:id :a/id, :age 42, :bar {:baz 42, :m 222}, :foo {3 4}}],

      "Empty overrides, passed as a map, result in no changes"
      sample-members
      {}
      sample-members,

      "Empty overrides, passed as nil, result in no changes"
      sample-members
      nil
      sample-members,

      "Empty overrides, passed as a vector, result in in an entire replacement of the members (empty case)"
      sample-members
      []
      [],

      "Empty overrides, passed as a vector, result in in an entire replacement of the members (non-empty case)"
      sample-members
      [1 {:foo :bar}]
      [1 {:foo :bar}],

      "Members can be removed"
      sample-members
      {:a/id nil}
      []

      "Reified members can be added"
      sample-members
      {:reified/id reified-member}
      (conj sample-members reified-member)

      "Members' scalar attributes can be removed"
      sample-members
      {:a/id {:age nil}}
      [{:id :a/id, :bar {:baz 42}}]

      "Deeply-located members' scalar attributes can be removed"
      sample-members
      {:a/id {:bar {:baz nil}}}
      [{:id :a/id, :age 42, :bar {}}]

      "Members' associative attributes can be removed"
      sample-members
      {:a/id {:bar nil}}
      [{:id :a/id, :age 42}]

      "One cannot override a non-existing member"
      sample-members
      {:b/id {:foo {3 4} :bar {:m 222}}}
      ::EXCEPTION)))

(deftest metadata-preservation
  (are [a b expected] (testing [a b]
                        (is (= expected
                               (-> (sut/removing-deep-merge a b)
                                   meta)))
                        true)
    ^{:a :b} {:c :d}, {:e :f},         {:a :b}
    nil               ^{:a :b} {:c :d} {:a :b}
    ^{:a :b} {:c :d}, nil              nil
    ^{:a :b} {:c :d}, {}               {:a :b}))
