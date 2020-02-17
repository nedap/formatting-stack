(ns formatting-stack.protocols.reporter
  (:require
   [formatting-stack.protocols.spec :as protocols.spec]
   [nedap.speced.def :as speced]))

(speced/defprotocol Reporter
  "A Reporter prints (or writes) info coming from other members, such as Formatters, Linters, etc."

  (report [this, ^::protocols.spec/reports reports]
    "Emits a report, out of the `reports` argument (namely a collection of discrete actionable items)."))
