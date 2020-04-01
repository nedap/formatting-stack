(ns functional.formatting-stack.component
  (:require
   [clojure.test :refer [deftest is testing]]
   [com.stuartsierra.component :as component]
   [formatting-stack.component :as sut]
   [formatting-stack.protocols.formatter :as protocols.formatter]
   [formatting-stack.protocols.linter :as protocols.linter]
   [formatting-stack.protocols.processor :as protocols.processor]
   [formatting-stack.reporters.passthrough :as reporters.passthrough]
   [nedap.utils.spec.api :refer [check!]]))

(def sample-report
  {:source   ::some-linter
   :level    :warning
   :column   40
   :line     1
   :msg      "omg"
   :filename "foo.clj"})

(defn proof [a]
  (reify formatting-stack.protocols.linter/Linter
    (--lint! [this filenames]
      (check! empty? filenames)
      (reset! a [sample-report]))))

(deftest works
  (testing "It can be started/stopped without errors"
    (let [p (atom nil)
          ;; pass an empty stack (except for the `proof`), so that no side-effects will be triggered (would muddy the test suite):
          opts {:strategies               []
                :third-party-indent-specs {}
                :formatters               ^{`protocols.formatter/--formatters (constantly [])} {}
                :linters                  ^{`protocols.linter/--linters (constantly [(proof p)])} {}
                :processors               ^{`protocols.processor/--processors (constantly [])} {}
                :in-background?           false
                :reporter                 (reporters.passthrough/new)}
          instance (sut/new opts)]
      (is (= instance
             (component/start instance)))

      (testing "It actually runs its members, such as linters"
        (is (= [sample-report]
               @p)))

      (is (= instance
             (component/stop instance))))))
