(ns unit.formatting-stack.formatters.clean-ns
  (:require
   [clojure.test :refer [deftest is testing]]
   [formatting-stack.formatters.clean-ns :as sut]
   [formatting-stack.strategies :as strategies]))

(when (strategies/refactor-nrepl-available?)
  (deftest with-memoized-libspec-allowlist
    (testing "Binds `*libspec-allowlist*`, which means that memoization will be effectively used"
      (let [bound? (fn []
                     @(resolve 'refactor-nrepl.ns.libspec-allowlist/*libspec-allowlist*))]
        (assert (not (bound?)))
        (is (sut/with-memoized-libspec-allowlist
              (bound?)))))))
