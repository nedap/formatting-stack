(ns functional.formatting-stack.component
  (:require
   [clojure.test :refer :all]
   [com.stuartsierra.component :as component]
   [formatting-stack.component :as sut]))

(deftest works
  (testing "It can be started/stopped without errors"
    (let [;; pass an empty stack, so that no side-effects will be triggered (would muddy the test suite):
          opts {:strategies               []
                :third-party-indent-specs {}
                :formatters               []
                :linters                  []
                :processors               []
                :in-background?           false
                :intersperse-newlines?    false}
          instance (sut/new opts)]
      (is (= instance
             (component/start instance)))

      (is (= instance
             (component/stop instance))))))
