(ns functional.formatting-stack.linters.eastwood
  (:require
   [clojure.test :refer [are deftest use-fixtures]]
   [formatting-stack.linters.eastwood :as sut]
   [formatting-stack.protocols.linter :as linter]
   [matcher-combinators.matchers :as matchers]
   [matcher-combinators.test :refer [match?]]))

(use-fixtures :once (fn [tests]
                      ;; prevent humongous AST representations from being printed:
                      (binding [*print-level* 5]
                        (tests))))

;; Important - these make `safe-the-ns` work, which is part of SUT's read-eval checking.
;; (these namespaces can be absent when running e.g. `lein test :only`)
(require 'functional.formatting-stack.linters.eastwood.examples.read-eval)
(require 'functional.formatting-stack.linters.eastwood.examples.read-eval-2)

(deftest lint!
  (let [linter (sut/new {})]
    (are [filename expected] (match? expected
                                     (linter/lint! linter [filename]))
      "test-resources/valid_syntax.clj"
      []

      "test-resources/invalid_syntax.clj"
      (matchers/equals
       [{:source    :formatting-stack/report-processing-error
         :level     :exception
         :msg       "Encountered an exception while running Eastwood"
         :exception #(= "Unmatched delimiter ]." (ex-message %))}])

      ;; if read-eval happens directly (without read-string being involved), then the exception is thrown by f-s:
      "test/functional/formatting_stack/linters/eastwood/examples/read_eval.clj"
      (matchers/equals
       [{:source    :formatting-stack/report-processing-error
         :level     :exception
         :msg       "Encountered an exception while running Eastwood"
         :exception #(= "#= not allowed when *read-eval* is false" (-> % ex-message))}])

      ;; if read-eval happens via read-string (triggered at compile-time), then the exception is thrown by Eastwood:
      "test/functional/formatting_stack/linters/eastwood/examples/read_eval_2.clj"
      (matchers/equals
       [{:source    :formatting-stack/report-processing-error
         :level     :exception
         :msg       "Encountered an exception while running Eastwood"
         :exception #(= "EvalReader not allowed when *read-eval* is false." (-> % ex-data :exception ex-message))}])

      "test-resources/eastwood_warning.clj"
      (matchers/in-any-order
       [{:source              :eastwood/warn-on-reflection
         :msg                 "reference to field getPath can't be resolved"
         :line                pos-int?
         :column              pos-int?
         :warning-details-url matchers/absent
         :filename            "test-resources/eastwood_warning.clj"}
        {:source              :eastwood/def-in-def
         :line                pos-int?
         :column              pos-int?
         :warning-details-url "https://github.com/jonase/eastwood#def-in-def"
         :filename            "test-resources/eastwood_warning.clj"}
        {:source              :eastwood/wrong-pre-post
         :line                pos-int?
         :column              pos-int?
         :warning-details-url "https://github.com/jonase/eastwood#wrong-pre-post"
         :filename            "test-resources/eastwood_warning.clj"}]))))
