(ns integration.formatting-stack.linters.one-resource-per-ns
  (:require
   [clojure.test :refer [are deftest is testing]]
   [formatting-stack.linters.one-resource-per-ns :as sut]
   [formatting-stack.test-helpers :as test-helpers]
   [formatting-stack.util :refer [rcomp]]))

(defn first! [coll]
  {:pre [(->> coll count #{1})]}
  (->> coll first))

(deftest analyze

  (testing "This namespace is unambiguous"
    (is (= (sut/analyze "test/integration/formatting_stack/linters/one_resource_per_ns.clj")
           ())))

  (testing "Sample files are ambiguous (on a per-extension basis)"
    (are [input extension expected] (testing input
                                      (is (= (into #{}
                                                   (map test-helpers/filename-as-resource)
                                                   expected)
                                             (->> input
                                                  sut/analyze
                                                  (filter (rcomp :extension #{extension}))
                                                  (first!)
                                                  :filenames
                                                  set)))
                                      true)
      "test-resources/orpn.clj"        ".clj"  #{"test-resources-extra/orpn.clj"
                                                 "test-resources/orpn.clj"}
      "test-resources-extra/orpn.clj"  ".clj"  #{"test-resources-extra/orpn.clj"
                                                 "test-resources/orpn.clj"}

      "test-resources/orpn.cljs"       ".cljs" #{"test-resources-extra/orpn.cljs"
                                                 "test-resources/orpn.cljs"}
      "test-resources-extra/orpn.cljs" ".cljs" #{"test-resources-extra/orpn.cljs"
                                                 "test-resources/orpn.cljs"}

      "test-resources/orpn.cljc"       ".cljc" #{"test-resources-extra/orpn.cljc"
                                                 "test-resources/orpn.cljc"}
      "test-resources-extra/orpn.cljc" ".cljc" #{"test-resources-extra/orpn.cljc"
                                                 "test-resources/orpn.cljc"})))
