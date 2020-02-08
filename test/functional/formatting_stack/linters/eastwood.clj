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
      ;; fixme should return a :reader-exception, currently lost in :note. lint! yields empty list
      #_#_"test-resources/invalid_syntax.clj"
        (matchers/embeds
         [{:level    :exception,
           :filename "test-resources/invalid_syntax.clj",
           :line     3,
           :column   2,
           :source   :eastwood/lint!}])

      "test-resources/eastwood_warning.clj"
      (matchers/embeds
       [{:source   :eastwood/def-in-def
         :line     3
         :column   13
         :filename "test-resources/eastwood_warning.clj"}]))))
