(defproject formatting-stack "0.2.0-SNAPSHOT"
  :description "An efficient, smart, graceful composition of formatters, linters and such."
  :url "https://github.com/nedap/formatting-stack"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[cljfmt "0.6.4"]
                 [integrant "0.7.0"]
                 [medley "1.1.0"]
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/tools.namespace "0.3.0-alpha4"]
                 [org.clojure/tools.reader "1.1.1"]
                 [com.gfredericks/how-to-ns "0.2.2"]
                 [com.stuartsierra/component "0.4.0"]])
