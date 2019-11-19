(ns sample-cljs-ns
  (:require
   [foo.bar.baz :as baz])
  (:require-macros
   [sample-cljs-ns :refer [the-macro]]))
