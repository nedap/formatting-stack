(ns unit.formatting-stack.util
  (:require
   [clojure.test :refer :all]
   [formatting-stack.util :as sut]))

(deftest read-ns-decl
  (are [input expected] (= expected
                           (sut/read-ns-decl input))
    "test-resources/sample_clj_ns.clj"   '(ns sample-clj-ns
                                            (:require [foo.bar.baz :as baz])
                                            (:import (java.util UUID)))
    "test-resources/sample_cljc_ns.cljc" '(ns sample-cljc-ns
                                            (:require [foo.bar.baz :as baz-clj])
                                            (:import (java.util UUID)))
    "test-resources/sample_cljs_ns.cljs" '(ns sample-cljs-ns
                                            (:require [foo.bar.baz :as baz])
                                            (:require-macros [sample-cljs-ns :refer [the-macro]]))))
