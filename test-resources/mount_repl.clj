(ns mount-repl
  "Documents a working setup for mount.

  Not a part of the test suite."
  (:require [formatting-stack.mount]
            [mount.core]))

(def sample-linters
  [(reify formatting-stack.protocols.linter/Linter
     (--lint! [this filenames]
       [{:source   ::my-linter
         :level    :warning
         :column   40
         :line     6
         :msg      "Hello, I am a sample linter!"
         :filename "path.clj"}]))])

(formatting-stack.mount/configure! {:linters sample-linters})

(comment
  (mount.core/start)
  (mount.core/start-with-args
   {:formatting-stack.mount/config {:linters sample-linters}}))
