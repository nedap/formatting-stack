(ns unit.formatting-stack.indent-specs
  (:require
   [clojure.test :refer :all]
   [formatting-stack.formatters.cljfmt.impl :as cljfmt.impl]
   [formatting-stack.formatters.cider :as formatters.cider]
   [formatting-stack.indent-specs :as sut]
   [formatting-stack.protocols.formatter :refer [format!]]))

(deftest default-third-party-indent-specs
  (testing "They can be processed without throwing errors, i.e. the quoted values are syntactically correct"

    (is (->> sut/default-third-party-indent-specs
             cljfmt.impl/cljfmt-third-party-indent-specs
             doall))

    (is (do (-> (formatters.cider/map->Formatter {:third-party-indent-specs sut/default-third-party-indent-specs})
                (format! []))
            true))))
