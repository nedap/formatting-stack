(ns functional.formatting-stack.formatters.clean-ns.should-not-be-cleaned
  (:require
   [clojure.java.io :as io]))

^{:foo ^{::io/foo 42} []} []
