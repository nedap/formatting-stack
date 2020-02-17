(ns unit.formatting-stack.indent-specs
  (:require
   [clojure.test :refer [deftest is testing]]
   [formatting-stack.formatters.cljfmt.impl :as cljfmt.impl]
   [formatting-stack.indent-specs :as sut]
   [formatting-stack.processors.cider :as processors.cider]
   [formatting-stack.protocols.processor :refer [process!]]))

(deftest default-third-party-indent-specs
  (testing "They can be processed without throwing errors, i.e. the quoted values are syntactically correct"

    (is (->> sut/default-third-party-indent-specs
             cljfmt.impl/cljfmt-third-party-indent-specs
             doall))

    (is (do (-> (processors.cider/new {:third-party-indent-specs sut/default-third-party-indent-specs})
                (process! []))
            true))))
