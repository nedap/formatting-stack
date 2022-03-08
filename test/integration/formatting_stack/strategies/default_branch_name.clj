(ns integration.formatting-stack.strategies.default-branch-name
  (:require
   [clojure.test :refer [deftest is]]
   [formatting-stack.strategies :as sut]))

;; This deftest lives in its own ns, for not having unrelated fixtures interfere
(deftest default-branch-name
  (is (= "main" (sut/default-branch-name))))
