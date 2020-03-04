(ns unit.formatting-stack.reporters.impl
  (:require
   [clojure.test :refer [are deftest is testing]]
   [formatting-stack.reporters.impl :as sut]))

(deftest normalize-filenames
  (let [cwd (System/getProperty "user.dir")]
    (are [input expected] (testing input
                            (is (= {:filename expected}
                                   (sut/normalize-filenames {:filename input})))
                            true)
      ""                      ""
      "a"                     "a"
      cwd                     ""
      (str cwd "/a.clj")      "a.clj"
      (str cwd "/a.clj" cwd), (str "a.clj" cwd))))
