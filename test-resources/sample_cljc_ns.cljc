(ns sample-cljc-ns
  (:require
   #?(:clj  [foo.bar.baz :as baz-clj]
      :cljs [foo.bar.baz :as baz-cljs]))
  #?(:clj
     (:import
      (java.util UUID)))
  #?(:cljs
     (:require-macros
      [sample-cljc-ns :refer [the-macro]])))
