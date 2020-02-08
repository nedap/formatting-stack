(ns unit.formatting-stack.formatters.trivial-ns-duplicates
  (:require
   [clojure.test :refer [are deftest is testing]]
   [formatting-stack.formatters.trivial-ns-duplicates :as sut]
   [formatting-stack.util.ns :as util.ns]))

(deftest maybe-remove-optionless-libspecs
  (are [input expected] (= expected
                           (sut/maybe-remove-optionless-libspecs input))
    '[[a] [a]]                      '[[a]]
    '[[a] [a :as foo]]              '[[a :as foo]]
    '[[a :as foo] [a]]              '[[a :as foo]]
    '[[a :as foo] [a :refer [bar]]] '[[a :as foo] [a :refer [bar]]]))

(deftest remove-refer
  (are [input expected] (= expected
                           (sut/remove-refer input))
    '[]                   '[]
    '[a]                  '[a]
    '[a :refer [a]]       '[a]
    '[a :refer [a] :as b] '[a :as b]
    '[a :as b :refer [a]] '[a :as b]))

(deftest maybe-remove-libspec-subsets
  (are [input expected] (= expected
                           (sut/maybe-remove-libspec-subsets input))
    '[]                                    '[]
    '[[a]]                                 '[[a]]
    '[[a] [a]]                             '[[a] [a]]
    '[[a :refer [b c]] [a :refer [b c d]]] '[[a :refer [b c d]]]))

(deftest remove-exact-duplicates
  (are [desc input expected] (testing desc
                               (is (= expected
                                      (sut/remove-exact-duplicates input)))
                               true)

    "returns nil when there's nothing to fix"
    '(ns foo)
    nil

    "ensures all libspecs are colls"
    '(ns foo (:require a))
    '(ns foo (:require [a]))

    "removes duplicates that only differ by their coll-ness"
    '(ns foo (:require a [a]))
    '(ns foo (:require [a]))

    "Removes duplicates if one of them has no options (:as clause), unwrapped"
    '(ns foo (:require a [a :as foo]))
    '(ns foo (:require [a :as foo]))

    "Removes duplicates if one of them has no options (:as clause), wrapped"
    '(ns foo (:require [a] [a :as foo]))
    '(ns foo (:require [a :as foo]))

    "Removes duplicates, if one of them has no options (`:refer` clause)"
    '(ns foo (:require [a] [a :refer [foo]]))
    '(ns foo (:require [a :refer [foo]]))

    "Does not remove non-exact duplicates, returning nil instead (`:as` clause)"
    '(ns foo (:require [a :as foo] [a :as bar]))
    nil

    "Does not remove non-exact duplicates, returning nil instead (`:refer` clause)"
    '(ns foo (:require [a :refer [foo]] [a :refer [bar]]))
    nil

    "Does not remove non-exact duplicates, returning nil instead (mixed `:as` and `:refer` clause)"
    '(ns foo (:require [a :as foo] [a :refer [bar]]))
    nil

    "Removes libspecs that are strict subsets of others"
    '(ns foo (:require [a :refer [a b]] [a :refer [a]]))
    '(ns foo (:require [a :refer [a b]]))

    "Removes libspecs that are strict subsets of others"
    '(ns foo (:require [a :refer [a b]] [a :refer [a]] [d]))
    '(ns foo (:require [a :refer [a b]] [d]))

    "Keeps libspecs which have :refer clauses that are a subset of others, but other different properties (like `:as`)"
    '(ns foo (:require [a :refer [a b]] [a :as b :refer [a]]))
    nil

    "Keeps libspecs which have :refer clauses that are a subset of others, but other different properties (like `:as`)"
    '(ns foo (:require [a :refer [a b]] [a :refer [a] :as b]))
    nil))

(deftest cljc-handling
  (are [input expected] (= (some-> expected util.ns/safely-read-ns-form)
                           (-> input
                               util.ns/safely-read-ns-form
                               sut/remove-exact-duplicates))

    "(ns foo (:require [#?(:clj foo :cljs bar)]))"
    nil

    "(ns foo (:require #?(:clj foo :cljs bar)))"
    nil

    "(ns foo (:require #?(:clj foo :cljs bar) :baz))"
    "(ns foo (:require [:baz] #?(:clj foo :cljs bar)))"

    "(ns foo (:require #?(:clj [foo] :cljs [bar])))"
    nil

    "(ns foo (:require [#?(:clj foo :cljs bar)] [#?(:clj foo :cljs bar)]))"
    "(ns foo (:require [#?(:clj foo :cljs bar)]))"

    "(ns foo (:require #?(:clj foo :cljs bar) #?(:clj foo :cljs bar)))"
    "(ns foo (:require #?(:clj foo :cljs bar)))"

    "(ns foo (:require #?(:clj foo :cljs bar) [#?(:clj foo :cljs bar)]))"
    nil))
