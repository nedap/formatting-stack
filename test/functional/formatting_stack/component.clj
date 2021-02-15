(ns functional.formatting-stack.component
  (:require
   [clojure.test :refer [deftest is testing]]
   [com.stuartsierra.component :as component]
   [formatting-stack.component :as sut]
   [formatting-stack.protocols.linter :as protocols.linter]
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
          opts {:overrides      {:strategies {:formatters []
                                              :linters    []
                                              :processors []}
                                 :formatters []
                                 :processors []
                                 :linters    [(proof p)]}
                :in-background? false
                :reporter       (reporters.passthrough/new)}
          instance (sut/new opts)]
      (is (= instance
             (component/start instance)))

      (testing "It actually runs its members, such as linters"
        (is (= [sample-report]
               @p)))

      (is (= instance
             (component/stop instance))))))
