(ns integrant-repl
  "Documents a working setup for Integrant, as a runnable example.

  Not a part of the test suite.

  See also:

    * https://github.com/nedap/formatting-stack/blob/master/README.md
    * https://github.com/nedap/formatting-stack/wiki/FAQ
    * The `customization-example` sibling namespace."
  (:require
   [clojure.spec.alpha :as spec]
   [formatting-stack.defaults]
   [formatting-stack.integrant]
   [formatting-stack.protocols.spec :as protocols.spec]
   [integrant.repl]))

(def sample-linters
  (conj formatting-stack.defaults/default-linters
        (reify formatting-stack.protocols.linter/Linter
          (--lint! [this filenames]
            [{:source   ::my-linter
              :level    :warning
              :column   40
              :line     6
              :msg      "Hello, I am a sample linter!"
              :filename "path.clj"}]))))

(integrant.repl/set-prep! (constantly {:formatting-stack.integrant/component {:linters sample-linters}}))

(comment
  (integrant.repl/reset))
