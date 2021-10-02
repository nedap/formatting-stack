(ns functional.formatting-stack.linters.kondo
  (:require
   [clojure.test :refer [are deftest]]
   [formatting-stack.linters.kondo :as sut]
   [formatting-stack.plugin :as plugin]
   [formatting-stack.protocols.linter :as linter]
   [matcher-combinators.matchers :as matchers]
   [matcher-combinators.test :refer [match?]]))

(deftest lint!
  (let [linter (sut/new {:kondo-clj-options {:output  {:exclude-files []}
                                             :linters {:unused-binding {:level :warning}}}})]
    (are [filename expected] (match? expected
                                     (linter/lint! linter [filename]))
      "test-resources/valid_syntax.clj"
      []

      "test-resources/invalid_syntax.clj"
      (matchers/embeds
       [{:level    :error,
         :filename "test-resources/invalid_syntax.clj",
         :line     1,
         :column   2,
         :source   :kondo/syntax}])

      "test-resources/kondo_warning.clj"
      (matchers/embeds
       [{:source   :kondo/unused-binding
         :level    :warning
         :line     3
         :column   7
         :filename "test-resources/kondo_warning.clj"}]))))


(deftest process
  (let [context {::sut/config {:output  {:exclude-files []}
                               :linters {:unused-binding {:level :warning}}}}]
    (are [filename expected] (match? expected
                                     (plugin/process :formatting-stack.linters/kondo
                                                     (assoc context :formatting-stack/files [filename])))
      "test-resources/valid_syntax.clj"
      {:formatting-stack/results []}

      "test-resources/invalid_syntax.clj"
      {:formatting-stack/results (matchers/embeds
                                  [{:level    :error,
                                    :filename "test-resources/invalid_syntax.clj",
                                    :line     1,
                                    :column   2,
                                    :source   :kondo/syntax}])}

      "test-resources/kondo_warning.clj"
      {:formatting-stack/results (matchers/embeds
                                  [{:source   :kondo/unused-binding
                                    :level    :warning
                                    :line     3
                                    :column   7
                                    :filename "test-resources/kondo_warning.clj"}])})))
