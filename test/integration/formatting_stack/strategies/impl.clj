(ns integration.formatting-stack.strategies.impl
  (:require
   [clojure.test :refer :all]
   [formatting-stack.strategies.impl :as sut])
  (:import
   (java.io File)))

(deftest dir-contains?
  (are [dirname filename expected] (= expected
                                      (sut/dir-contains? dirname filename))

    "."                             (-> "src/formatting_stack/strategies/impl.clj" File.) true
    (-> "." File. .getAbsolutePath) (File. "project.clj")                                 true
    "."                             (File. "project.clj")                                 true
    "."                             (File. "dev/user.clj")                                true
    "dev"                           (File. "dev/user.clj")                                true
    "dev"                           (File. "user.clj")                                    false))
