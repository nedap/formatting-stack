(defproject formatting-stack "1.0.0-alpha2"
  :description "An efficient, smart, graceful composition of formatters, linters and such."

  :url "https://github.com/nedap/formatting-stack"

  :license {:name "EPL-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}

  :signing {:gpg-key "releases-staffingsolutions@nedap.com"}

  :repositories {"releases" {:url      "https://nedap.jfrog.io/nedap/staffing-solutions/"
                             :username :env/artifactory_user
                             :password :env/artifactory_pass}}

  :deploy-repositories {"clojars" {:url      "https://clojars.org/repo"
                                   :username :env/clojars_user
                                   :password :env/clojars_pass}}

  :dependencies [[cljfmt "0.6.5" :exclusions [rewrite-clj]]
                 [com.gfredericks/how-to-ns "0.2.6"]
                 [com.gfredericks/lein-all-my-files-should-end-with-exactly-one-newline-character "0.1.1"]
                 [com.nedap.staffing-solutions/speced.def "1.0.0"]
                 [com.nedap.staffing-solutions/utils.collections "2.0.0-alpha3"]
                 [com.stuartsierra/component "0.4.0"]
                 [integrant "0.7.0"]
                 [clj-kondo "2019.05.19-alpha"]
                 [jonase/eastwood "0.3.5"]
                 [lein-bikeshed "0.5.1"]
                 [medley "1.1.0"]
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.namespace "0.3.0-alpha4"]
                 [org.clojure/tools.reader "1.2.2"]
                 [refactor-nrepl "2.4.0"]
                 [rewrite-clj "0.6.1"]]

  :profiles {:dev      {:source-paths   ["dev"]
                        :resource-paths ["test-resources"]
                        :plugins        [[lein-cloverage "1.0.13"]]}
             ;; dedicated `:pedantic` profile since the `lein-cloverage` plugin would introduce faults,
             ;; and plugins don't accept :exclusions
             :pedantic {:pedantic? :abort}
             :ci       {:plugins      [[cider/cider-nrepl "0.21.1"]]
                        :jvm-opts     ["-Dclojure.main.report=stderr"]
                        :global-vars  {*assert* true} ;; `ci.release-workflow` relies on runtime assertions
                        :dependencies [[com.nedap.staffing-solutions/ci.release-workflow "1.3.0-alpha3"]]}
             ;; `dev` in :test is important - a test depends on it:
             :test     {:source-paths   ["dev"]
                        :resource-paths ["test-resources"]}})
