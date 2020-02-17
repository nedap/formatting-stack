(ns functional.formatting-stack.linters.how-to-ns
  (:require
   [clojure.test :refer [are deftest]]
   [formatting-stack.formatters.how-to-ns :as sut]
   [formatting-stack.protocols.linter :as linter]
   [matcher-combinators.test :refer [match?]]))

(deftest lint!
  (let [linter (sut/new {})]
    (are [filename expected] (match? expected
                                     (linter/lint! linter [filename]))
      "test-resources/invalid_syntax.clj"
      [{:source :formatting-stack/report-processing-error
        :level :exception
        :filename "test-resources/invalid_syntax.clj"}]

      "test-resources/ns_unordered.clj"
      [{:source :how-to-ns/ns
        :msg "Badly formatted namespace"
        :diff string?
        :filename "test-resources/ns_unordered.clj"}])))
