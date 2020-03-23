(ns functional.formatting-stack.integrant
  (:require
   [clojure.test :refer [deftest is testing]]
   [formatting-stack.integrant :as sut]
   [formatting-stack.reporters.passthrough :as reporters.passthrough]
   [integrant.core :as integrant]
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
                :formatters               (constantly [])
                :linters                  (constantly [(proof p)])
                :processors               (constantly [])
                :in-background?           false
                :reporter                 (reporters.passthrough/new)}
          system {:formatting-stack.integrant/component opts}
          started-system (integrant/init system)]

      (testing "It actually runs its members, such as linters"
        (is (= [sample-report]
               @p)))

      (integrant/halt! started-system))))
