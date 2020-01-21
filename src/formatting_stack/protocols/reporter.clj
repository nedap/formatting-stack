(ns formatting-stack.protocols.reporter
  (:require
   [formatting-stack.protocols.spec :as protocols.spec]
   [nedap.speced.def :as speced]))

(speced/defprotocol Reporter
  "" ;; FIXME

  (report [this, ^::protocols.spec/reports reports]
    "")) ;; FIXME
