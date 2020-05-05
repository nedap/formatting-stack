(ns functional.formatting-stack.core
  (:require
   [clojure.test :refer :all]
   [formatting-stack.core :as sut]
   [formatting-stack.protocols.formatter :as formatter]
   [formatting-stack.protocols.reporter :as protocols.reporter]
   [matcher-combinators.test :refer [match?]]
   [nedap.utils.modular.api :refer [implement]])
  (:import
   (clojure.lang ExceptionInfo)))

(defn throw-exception [& args]
  (throw (ex-info "Kaboom!" {})))

(def failing-formatter
  (implement {:id ::failing-formatter}
    formatter/--format! throw-exception))

(defn store-in-latest [{::keys [latest]} args]
  (reset! latest args))

(defn recording-reporter [latest]
  (implement {::latest latest}
    protocols.reporter/--report store-in-latest))

(deftest format!
  (testing "exceptions are passed to the reporter"
    (let [latest (atom nil)]
      (sut/format! :formatters [failing-formatter]
                   :linters []
                   :processors []
                   :reporter (recording-reporter latest)
                   :in-background? false)
      (is (match? [{:source    :formatting-stack/process!
                    :msg       "Exception during {:id :functional.formatting-stack.core/failing-formatter}"
                    :level     :exception
                    :exception #(instance? ExceptionInfo %)}]
                  @latest)))))
