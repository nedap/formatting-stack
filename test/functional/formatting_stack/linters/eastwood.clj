(ns functional.formatting-stack.linters.eastwood
  (:require
   [clojure.test :refer [are deftest]]
   [formatting-stack.linters.eastwood :as sut]
   [formatting-stack.protocols.linter :as linter]
   [matcher-combinators.matchers :as matchers]
   [matcher-combinators.test :refer [match?]]))

(deftest lint!
  (let [linter (sut/new {})]
    (are [filename expected] (match? expected
                                     (linter/lint! linter [filename]))
      "test-resources/invalid-syntax.clj"
      []

      "test-resources/eastwood_warning.clj"
      (matchers/embeds
       [{:source   :eastwood/def-in-def
         :line     3
         :column   13
         :filename "test-resources/eastwood_warning.clj"}]))))
