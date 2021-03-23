(ns unit.formatting-stack.linters.eastwood.impl
  (:require
   [clojure.string :as str]
   [clojure.test :refer [are deftest is testing]]
   [formatting-stack.linters.eastwood.impl :as sut]
   [matcher-combinators.test :refer [match?]])
  (:import
   (java.io File)))

(deftest contains-dynamic-assertions?
  (testing "matches on false-positive wrong-pre-post reports"
    (are [form expected] (testing (pr-str form)
                           (is (= expected
                                  (sut/contains-dynamic-assertions?
                                   {:wrong-pre-post {:ast {:form form}}
                                    :msg (str "Precondition found that is probably always logical true or always logical false."
                                              "Should be changed to function call?  *test*")})))
                           true)
      (list 'fn* (list [] (list 'clojure.core/assert '*test*)))           true

      (list 'fn* (list [] (list 'clojure.core/assert 'namespace/*test*))) true

      (list 'fn*
            (list []
                  (list 'clojure.core/assert '(foo 42))
                  (list 'clojure.core/assert '*test*)))                   true

      (list 'fn*
            (list []
                  (list 'clojure.core/assert '(foo 42))
                  (list 'clojure.core/assert 'namespace/*test*)))         true

      nil                                                                 false
      []                                                                  false
      42                                                                  false
      [42]                                                                false
      [[42]]                                                              false
      [[[42]]]                                                            false
      [:_ [:_]]                                                           false
      [:_ [:_ []]]                                                        false
      [:_ [:_ 'aaaaaaaaa]]                                                false

      (list 'fn* (list [] (list 'clojure.core/assert 'test)))             false

      (list 'fn* (list [] (list 'clojure.core/assert '*namespace*/test))) false

      (list 'fn* (list [] (list 'clojure.core/assert '*namespace/test*))) false

      (list 'fn* (list [] (list 'clojure.core/assert 1)))                 false
      (list 'fn* (list [] (list 'clojure.core/assert [])))                false
      (list 'fn* (list [] (list 'clojure.core/assert {})))                false
      (list 'fn* (list [] (list 'clojure.core/assert "test")))            false
      (list 'fn* (list [] (list 'clojure.core/assert "foo/test")))        false))

  (testing "can handle variable input"
    (are [input] (= false
                    (sut/contains-dynamic-assertions? input))
      nil
      {}
      {:test 1}
      {:wrong-pre-post {:ast nil}}
      {:wrong-pre-post {:ast {:form []}}}
      {:wrong-pre-post {:ast {:form '(fn* ([] (clojure.core/assert (true? true))))}}})))

(deftest warnings->report
  (are [input expected] (match? expected
                                (sut/warnings->reports input))
    ""
    []

    "path.clj:6:40: Reflection warning - reference to field getPath can't be resolved."
    [{:source   :eastwood/warn-on-reflection
      :level    :warning
      :column   40
      :line     6
      :msg      "reference to field getPath can't be resolved"
      :filename (-> "path.clj" File. .getCanonicalPath)}]

    (str/join "\n"
              ["path.clj:6:40: Reflection warning - reference to field getPath can't be resolved."
               "other-path.clj:13:12: Reflection warning - different message."])
    [{:source   :eastwood/warn-on-reflection
      :level    :warning
      :column   40
      :line     6
      :msg      "reference to field getPath can't be resolved"
      :filename (-> "path.clj" File. .getCanonicalPath)}
     {:source   :eastwood/warn-on-reflection
      :level    :warning
      :column   12
      :line     13
      :msg      "different message"
      :filename (-> "other-path.clj" File. .getCanonicalPath)}]

    (str/join "\n"
              ["path.clj:6:40: Reflection warning - reference to field getPath can't be resolved."
               "random garbage"
               "path.clj: Incomplete warning - different message."
               "other-path.clj:13:12: Reflection warning - different message."])
    [{:msg      "reference to field getPath can't be resolved"
      :filename (-> "path.clj" File. .getCanonicalPath)}
     {:msg      "different message"
      :filename (-> "other-path.clj" File. .getCanonicalPath)}]))
