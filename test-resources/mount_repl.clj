(ns mount-repl
  "Documents a working setup for `mount`.

  Not a part of the test suite."
  (:require
   [formatting-stack.mount]
   [mount.core]))

(def sample-linters-1
  [(reify formatting-stack.protocols.linter/Linter
     (--lint! [this filenames]
       [{:source   ::foo-linter
         :level    :warning
         :column   40
         :line     6
         :msg      "Hello, I am Linter 1!"
         :filename "path.clj"}]))])

(def sample-linters-2
  [(reify formatting-stack.protocols.linter/Linter
     (--lint! [this filenames]
       [{:source   ::bar-linter
         :level    :warning
         :column   40
         :line     6
         :msg      "Hello, I am Linter 2!"
         :filename "path.clj"}]))])

(formatting-stack.mount/configure! {:linters sample-linters-1})

(comment

  (mount.core/start)

  (mount.core/start-with-args
   {:formatting-stack.mount/config {:linters sample-linters-2}}))
