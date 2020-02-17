(ns unit.formatting-stack.linters.eastwood.impl
  (:require
   [clojure.string :as str]
   [clojure.test :refer [are deftest]]
   [formatting-stack.linters.eastwood.impl :as sut]
   [matcher-combinators.test :refer [match?]]))

(deftest warnings->report
  (are [input expected] (match? expected
                                (sut/warnings->reports input))
    ""
    []

    "path.clj:6:40: Reflection warning - reference to field getPath can't be resolved."
    [{:source :eastwood/warn-on-reflection
      :level :warning
      :column 40
      :line 6
      :msg "reference to field getPath can't be resolved"
      :filename "path.clj"}]

    (str/join "\n"
              ["path.clj:6:40: Reflection warning - reference to field getPath can't be resolved."
               "other-path.clj:13:12: Reflection warning - different message."])
    [{:source :eastwood/warn-on-reflection
      :level :warning
      :column 40
      :line 6
      :msg "reference to field getPath can't be resolved"
      :filename "path.clj"}
     {:source :eastwood/warn-on-reflection
      :level :warning
      :column 12
      :line 13
      :msg "different message"
      :filename "other-path.clj"}]

    (str/join "\n"
              ["path.clj:6:40: Reflection warning - reference to field getPath can't be resolved."
               "random garbage"
               "path.clj: Incomplete warning - different message."
               "other-path.clj:13:12: Reflection warning - different message."])
    [{:msg "reference to field getPath can't be resolved"
      :filename "path.clj"}
     {:msg "different message"
      :filename "other-path.clj"}]))
