(defproject formatting-stack "0.17.1"
  :description "An efficient, smart, graceful composition of formatters, linters and such."
  :url "https://github.com/nedap/formatting-stack"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories {"releases" {:url      "https://nedap.jfrog.io/nedap/staffing-solutions/"
                             :username :env/artifactory_user
                             :password :env/artifactory_pass}}
  :dependencies [[cljfmt "0.6.4"]
                 [com.gfredericks/how-to-ns "0.2.2"]
                 [com.gfredericks/lein-all-my-files-should-end-with-exactly-one-newline-character "0.1.1"]
                 [com.nedap.staffing-solutions/utils.collections "0.3.1"]
                 [com.stuartsierra/component "0.4.0"]
                 [integrant "0.7.0"]
                 [clj-kondo "2019.05.19-alpha"]
                 [jonase/eastwood "0.3.5"]
                 [lein-bikeshed "0.5.1"]
                 [medley "1.1.0"]
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/tools.namespace "0.3.0-alpha4"]
                 [org.clojure/tools.reader "1.1.1"]
                 [refactor-nrepl "2.4.0"]]
  :profiles {:dev  {:source-paths   ["dev"]
                    :resource-paths ["test-resources"]}
             ;; `dev` in :test is important - a test depends on it:
             :test {:source-paths   ["dev"]
                    :resource-paths ["test-resources"]}})
