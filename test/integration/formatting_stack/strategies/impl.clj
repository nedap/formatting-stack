(ns integration.formatting-stack.strategies.impl
  (:require
   [clojure.test :refer [are deftest is testing]]
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
    "."                             (File. "LICENSE")                                     true
    (-> "." File. .getAbsolutePath) (File. "LICENSE")                                     true
    "."                             (File. "./LICENSE")                                   true
    "dev"                           (File. "LICENSE")                                     false
    "dev"                           (File. "./LICENSE")                                   false
    (-> "." File. .getAbsolutePath) (File. "I_dont_exist")                                false
    "."                             (File. "I_dont_exist")                                false
    "dev"                           (File. "I_dont_exist")                                false
    "dev"                           (File. "user.clj")                                    false))

(deftest absolutize
  (are [target] (testing target
                  (is (= [(-> target File. .getCanonicalPath)]
                         (sut/absolutize "git" [target])))
                  true)
    "src/formatting_stack/strategies/impl.clj"
    "src/../src/formatting_stack/strategies/impl.clj")

  (is (spec-assertion-thrown? ::sut/existing-files (sut/absolutize "git" ["I_dont_exist"]))))
