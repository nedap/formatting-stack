(ns integrant-repl
  "Documents a working setup for Integrant.

  Not a part of the test suite."
  (:require
   [clojure.spec.alpha :as spec]
   [formatting-stack.integrant]
   [formatting-stack.protocols.spec :as protocols.spec]
   [integrant.repl]))

(def sample-linters
  [(reify formatting-stack.protocols.linter/Linter
     (--lint! [this filenames]
       [{:source   ::my-linter
         :level    :warning
         :column   40
         :line     6
         :msg      "Hello, I am a sample linter!"
         :filename "path.clj"}]))])

(integrant.repl/set-prep! (constantly {:formatting-stack.integrant/component {:linters sample-linters}}))

(comment
  (integrant.repl/reset))
