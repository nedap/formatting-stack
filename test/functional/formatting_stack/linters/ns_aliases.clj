(ns functional.formatting-stack.linters.ns-aliases
  (:require
   [clojure.test :refer [are deftest testing]]
   [formatting-stack.linters.ns-aliases :as sut]
   [formatting-stack.linters.ns-aliases.impl :as sut.impl]
   [formatting-stack.protocols.linter :as linter]
   [formatting-stack.strategies :as strategies]
   [matcher-combinators.test :refer [match?]]))

(deftest lint!
  (let [linter (sut/new {:max-lines-per-ns 4
                         :augment-acceptable-aliases-whitelist? false})]
    (are [filename expected] (match? expected
                                     (linter/lint! linter [filename]))
      "test-resources/valid_syntax.clj"
      []

      "test-resources/invalid_syntax.clj"
      [] ;; reader exceptions are ignored

      "test-resources/ns_aliases_warning.clj"
      [{:source              :formatting-stack/ns-aliases
        :line                3
        :column              4
        :warning-details-url "https://stuartsierra.com/2015/05/10/clojure-namespace-aliases"
        :msg                 "[clojure.string :as foo] is not a derived alias."
        :filename            "test-resources/ns_aliases_warning.clj"}]))

  (when (strategies/refactor-nrepl-3-4-1-available?)
    (testing "`:augment-acceptable-aliases-whitelist?`"
      (are [input expected] (match? expected
                                    (linter/lint! (sut/new {:augment-acceptable-aliases-whitelist? input})
                                                  ["test-resources/ns_aliases_warning.clj"]))
        true  []
        false [{:source              :formatting-stack/ns-aliases
                :line                3
                :column              4
                :warning-details-url "https://stuartsierra.com/2015/05/10/clojure-namespace-aliases"
                :msg                 "[clojure.string :as foo] is not a derived alias."
                :filename            "test-resources/ns_aliases_warning.clj"}]))))
