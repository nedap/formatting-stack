(ns component-repl
  "Documents a working setup for Component.

  Not a part of the test suite."
  (:require
   [com.stuartsierra.component :as component]
   [com.stuartsierra.component.repl :as component.repl]
   [formatting-stack.component]))

(def sample-linters
  [(reify formatting-stack.protocols.linter/Linter
     (--lint! [this filenames]
       [{:source   ::my-linter
         :level    :warning
         :column   40
         :line     6
         :msg      "Hello, I am a sample linter!"
         :filename "path.clj"}]))])

(defn init [_]
  (component/system-map :formatting-stack (formatting-stack.component/new {:linters sample-linters})))

(component.repl/set-init init)

(comment
  (component.repl/reset))
