(ns unit.formatting-stack.linters.ns-aliases
  (:require
   [clojure.test :refer :all]
   [formatting-stack.linters.ns-aliases :as sut]))

(deftest name-and-alias
  (are [input expected] (= expected
                           (sut/name-and-alias input))
    '[foo],                     ['foo nil]
    '[foo :refer :all],         ['foo nil]
    '[foo :refer :all :as bar], ['foo 'bar]
    '[foo :as bar :refer :all], ['foo 'bar]
    '[foo :as bar],             ['foo 'bar]
    '["foo" :as bar],           ["foo" 'bar]))

(deftest derived?
  (are [ns-name _as alias expected] (= expected
                                       (sut/derived? alias :from ns-name))
    'com.enterprise.foo.bar :as 'bar,                    true
    'com.enterprise.foo.bar :as 'foo.bar,                true
    'com.enterprise.foo.bar :as 'enterprise.foo.bar,     true
    'com.enterprise.foo.bar :as 'com.enterprise.foo.bar, true

    'com.enterprise.foo.bar :as 'com.enterprise.foo,     false
    'com.enterprise.foo.bar :as 'com.enterprise,         false
    'com.enterprise.foo.bar :as 'com,                    false
    'com.enterprise.foo.bar :as 'enterprise,             false
    'com.enterprise.foo.bar :as 'foo,                    false

    'com.enterprise.foo.bar :as 'b,                      false

    'net.xyz.core           :as 'xyz                     true
    'net.xyz.core           :as 'net.xyz                 true

    'net.xyz.alpha          :as 'xyz                     true
    'net.xyz.alpha          :as 'net.xyz                 true

    'net.xyz.alpha.core     :as 'xyz                     true
    'net.xyz.alpha.core     :as 'net.xyz                 true

    'net.xyz.core.alpha     :as 'xyz                     true
    'net.xyz.core.alpha     :as 'net.xyz                 true

    'buddy.core.keys        :as 'buddy.keys              true
    'buddy.core.keys        :as 'buddy.core.keys         true
    'buddy.core.keys        :as 'keys.buddy              false

    'clj-http               :as 'http                    true
    'clj-http               :as 'htta                    false
    'clj-time.core          :as 'time                    true
    'clj-time.core          :as 'tima                    false
    'clj-time.format        :as 'time.format             true
    'clj-time.format        :as 'time.farmat             false))

(deftest acceptable-require-clause?
  (are [whitelist input expected] (= expected
                                     (sut/acceptable-require-clause? whitelist input))

    sut/default-acceptable-aliases-whitelist 'foo                 true
    sut/default-acceptable-aliases-whitelist '[foo]               true
    sut/default-acceptable-aliases-whitelist '[foo :as foo]       true
    sut/default-acceptable-aliases-whitelist '[foo :as bar]       false

    sut/default-acceptable-aliases-whitelist '[foo :as sut]       true
    {}                                       '[foo :as sut]       false

    sut/default-acceptable-aliases-whitelist '[datomic.api :as d] true
    {}                                       '[datomic.api :as d] false))
