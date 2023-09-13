(ns unit.formatting-stack.formatters.cljfmt.impl.magic-symbols
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer [are deftest is]]
   [formatting-stack.formatters.cljfmt.impl :as sut]))

(deftest works
  (are [filename has-magic-indent?] (let [indents (sut/cljfmt-indents-for filename {})
                                          action (-> indents (get 'action))]
                                      (is (> (count indents) 10))
                                      (if has-magic-indent?
                                        (is (= [[:inner 0]]
                                               action))
                                        (is (nil? action))))

    nil
    false

    (-> (io/file "test-resources" "magic.clj")
        (.getCanonicalPath))
    true))
