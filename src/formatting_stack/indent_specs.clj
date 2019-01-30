(ns formatting-stack.indent-specs)

(def default-third-party-indent-specs
  '{hiccup.page/html5 {:style/indent 1}
    hiccup.form/form-to {:style/indent 1}
    clojure.test.check.properties/for-all {:style/indent 1}
    fulcro-spec.core/specification {:style/indent 1}
    fulcro-spec.core/behavior {:style/indent 1}
    fulcro-spec.core/assertions {:style/indent 0
                                 :style.cljfmt/type :inner}
    garden.stylesheet/at-media {:style/indent 1}})
