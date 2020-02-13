(ns functional.formatting-stack.linters.ns-aliases
  (:require
   [clojure.test :refer [are deftest]]
   [formatting-stack.linters.ns-aliases :as sut]
   [formatting-stack.protocols.linter :as linter]
   [matcher-combinators.test :refer [match?]]))

(deftest lint!
  (let [linter (sut/new {:max-lines-per-ns 4})]
    (are [filename expected] (match? expected
                                     (linter/lint! linter [filename]))
      "test-resources/invalid_syntax.clj"
      [] ;; reader exceptions are ignored

      "test-resources/ns_aliases_warning.clj"
      [{:source              :formatting-stack/ns-aliases
        :line                3
        :column              4
        :warning-details-url "https://stuartsierra.com/2015/05/10/clojure-namespace-aliases"
        :msg                 "[clojure.string :as foo] is not a derived alias."
        :filename            "test-resources/ns_aliases_warning.clj"}])))
