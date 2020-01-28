(ns functional.formatting-stack.linters.kondo
  (:require
   [clojure.test :refer :all]
   [formatting-stack.linters.kondo :as sut]
   [formatting-stack.protocols.linter :as linter]
   [matcher-combinators.matchers :as m]
   [matcher-combinators.test :refer [match?]]))

(deftest lint!
  (let [linter (sut/new {:kondo-options {:output {:exclude-files []}}})]
    (are [filename expected] (match? expected
                                     (linter/lint! linter [filename]))
      "test-resources/invalid_syntax.clj"
      (m/embeds
       [{:level :error,
         :filename "test-resources/invalid_syntax.clj",
         :line 1,
         :column 2,
         :source :kondo/syntax}])

      "test-resources/kondo_warning.clj"
      (m/embeds
       [{:source :kondo/unused-binding
         :level :warning
         :line 3
         :column 7
         :filename "test-resources/kondo_warning.clj"}]))))
