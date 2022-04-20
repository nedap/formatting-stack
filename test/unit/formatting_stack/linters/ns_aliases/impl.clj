(ns unit.formatting-stack.linters.ns-aliases.impl
  (:require
   [clojure.test :refer [are deftest]]
   [formatting-stack.linters.ns-aliases.impl :as sut]
   [matcher-combinators.matchers :as matchers]
   [matcher-combinators.test :refer [match?]]))

(deftest merge-aliases
  (are [m1 m2 expected] (match? expected
                                (sut/merge-aliases m1 m2))
    {}                      {}                      {}
    {'a ['b]}               {}                      {'a ['b]}
    {}                      {'a ['b]}               {'a ['b]}
    {'a ['b]}               {'a ['c]}               {'a (matchers/in-any-order ['b 'c])}
    {'a ['b]}               {'a ['b]}               {'a ['b]}
    {'a ['b 'c] 'd ['e 'f]} {'a ['g 'h] 'd ['i 'j]} {'a (matchers/in-any-order ['b 'c 'g 'h])
                                                     'd (matchers/in-any-order ['e 'f 'i 'j])}
    {'a ['b 'c] 'd ['e 'f]} {'a ['b 'c] 'd ['e 'f]} {'a (matchers/in-any-order ['b 'c])
                                                     'd (matchers/in-any-order ['e 'f])}))
