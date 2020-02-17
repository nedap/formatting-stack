(ns unit.formatting-stack.formatters.no-extra-blank-lines
  (:require
   [clojure.test :refer [are deftest]]
   [formatting-stack.formatters.no-extra-blank-lines :as sut]))

(deftest without-extra-newlines
  (are [i e] (= e
                (sut/without-extra-newlines i))
    "\n"
    "\n"

    "\n\n"
    "\n\n"

    "\n\n\n"
    "\n\n"

    "\n\n\n\n\n\n\n\n"
    "\n\n"

    "a\n\n\na"
    "a\n\na"

    "\n\na\n"
    "\n\na\n"))
