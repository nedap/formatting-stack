(ns functional.formatting-stack.mount
  (:require
   [clojure.test :refer [deftest is testing]]
   [formatting-stack.mount :as sut]
   [formatting-stack.reporters.passthrough :as reporters.passthrough]
   [mount.core :as mount :refer [defstate]]
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

(defstate stateful-component
  :start 42
  :stop 0)

(deftest works
  (testing "It can be started/stopped without errors"
    (let [p (atom nil)
          ;; pass an empty stack (except for the `proof`), so that no side-effects will be triggered (would muddy the test suite):
          opts {:strategies               []
                :third-party-indent-specs {}
                :formatters               []
                :linters                  [(proof p)]
                :processors               []
                :in-background?           false
                :reporter                 (reporters.passthrough/new)}]
      (sut/configure! opts)
      (mount/start)
      (testing "It actually runs its members, such as linters"
        (is (= [sample-report]
               @p)))
      (mount/stop))))
