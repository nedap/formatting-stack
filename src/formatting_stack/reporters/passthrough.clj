(ns formatting-stack.reporters.passthrough
  (:require
   [formatting-stack.protocols.reporter :as protocols.reporter]
   [nedap.utils.modular.api :refer [implement]]))

(defn report [_ _]
  [])

(defn new
  "Does not perform a report. Apt for the test suite."
  []
  (implement {}
    protocols.reporter/--report report))
