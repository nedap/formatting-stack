(ns formatting-stack.component
  (:require
   [com.stuartsierra.component :as component]
   [formatting-stack.core :refer [format!]]))

(defrecord Formatter [strategies how-to-ns-opts cljfmt-opts third-party-indent-specs]
  component/Lifecycle
  (start [this]
    (format! :strategies strategies
             :how-to-ns-opts how-to-ns-opts
             :cljfmt-opts cljfmt-opts
             :third-party-indent-specs third-party-indent-specs)
    this)

  (stop [this]
    this))
