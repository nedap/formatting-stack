;; Please don't bump the library version by hand - use ci.release-workflow instead.
(defproject formatting-stack "4.3.0"
  ;; Please keep the dependencies sorted a-z.
  :dependencies [[clj-kondo "2021.01.20"]
                 [cljfmt "0.7.0"]
                 [com.gfredericks/how-to-ns "0.2.8"]
                 [com.gfredericks/lein-all-my-files-should-end-with-exactly-one-newline-character "0.1.1"]
                 [com.nedap.staffing-solutions/speced.def "2.0.0"]
                 [com.nedap.staffing-solutions/utils.collections "2.1.0"]
                 [com.nedap.staffing-solutions/utils.modular "2.2.0-alpha3"]
                 [com.nedap.staffing-solutions/utils.spec.predicates "1.1.0"]
                 [io.reflectoring.diffparser/diffparser "1.4"]
                 [jonase/eastwood "0.3.14"]
                 [medley "1.2.0"]
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/java.classpath "1.0.0"]
                 [org.clojure/java.data "1.0.64"]
                 [org.clojure/tools.namespace "0.3.1"]
                 [org.clojure/tools.reader "1.3.4"]]

  :managed-dependencies [[rewrite-clj "0.6.1"]]

  ;; The f-s exclusion allows adding f-s in a global profile, while still allowing developing f-s itself,
  ;; avoiding having the global version shadow the local one
  :exclusions [formatting-stack]

  :description "An efficient, smart, graceful composition of formatters, linters and such."

  :url "https://github.com/nedap/formatting-stack"

  :min-lein-version "2.0.0"

  :license {:name "EPL-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}

  :signing {:gpg-key "releases-staffingsolutions@nedap.com"}

  :repositories {"releases" {:url      "https://nedap.jfrog.io/nedap/staffing-solutions/"
                             :username :env/artifactory_user
                             :password :env/artifactory_pass}}

  :repository-auth {#"https://nedap.jfrog\.io/nedap/staffing-solutions/"
                    {:username :env/artifactory_user
                     :password :env/artifactory_pass}}

  :deploy-repositories {"clojars" {:url      "https://clojars.org/repo"
                                   :username :env/clojars_user
                                   :password :env/clojars_pass}}

  :source-paths ["src" "worker"]

  :target-path "target/%s"

  :test-paths ["src" "test"]

  :monkeypatch-clojure-test false

  :plugins [[lein-pprint "1.1.2"]]

  ;; A variety of common dependencies are bundled with `nedap/lein-template`.
  ;; They are divided into two categories:
  ;; * Dependencies that are possible or likely to be needed in all kind of production projects
  ;;   * The point is that when you realise you needed them, they are already in your classpath, avoiding interrupting your flow
  ;;   * After realising this, please move the dependency up to the top level.
  ;; * Genuinely dev-only dependencies allowing 'basic science'
  ;;   * e.g. criterium, deep-diff, clj-java-decompiler

  ;; Manage transitive deps using :managed-dependencies, see https://git.io/JtUGI
  :profiles {:dev                   {:dependencies [[com.clojure-goes-fast/clj-java-decompiler "0.2.1"]
                                                    [com.stuartsierra/component "0.4.0"]
                                                    [com.taoensso/timbre "4.10.0"]
                                                    [criterium "0.4.5"]
                                                    [integrant/repl "0.3.1"]
                                                    [lambdaisland/deep-diff "0.0-29"]
                                                    [org.clojure/core.async "0.5.527"]
                                                    [org.clojure/math.combinatorics "0.1.1"]
                                                    [org.clojure/test.check "0.10.0-alpha3"]]
                                     :jvm-opts     ["-Dclojure.compiler.disable-locals-clearing=true"]
                                     :source-paths ["dev"]
                                     :repl-options {:init-ns dev}
                                     :middleware   [~(do ;; the following ensures that :exclusions are honored in all cases
                                                       (create-ns 'user)
                                                       (intern 'user
                                                               'nedap-ensure-exclusions
                                                               (fn [project]
                                                                 (let [exclusions (->> project
                                                                                       :exclusions
                                                                                       (map (fn [x]
                                                                                              (str (if (namespace (symbol x))
                                                                                                     x
                                                                                                     (symbol (str x) (str x))))))
                                                                                       (set))]
                                                                   (update project :dependencies (fn [deps]
                                                                                                   (->> deps
                                                                                                        (remove (fn [[dep version]]
                                                                                                                  (exclusions (str dep))))
                                                                                                        vec))))))
                                                       'user/nedap-ensure-exclusions)]}

             :cljs-old              {:dependencies [[cljfmt "0.6.5"]
                                                    [com.stuartsierra/component "0.4.0"]
                                                    [integrant "0.8.0"]
                                                    [org.clojure/clojurescript "1.7.228"]]}

             :provided              {:dependencies [[org.clojure/clojurescript "1.10.597"]
                                                    [com.stuartsierra/component "0.4.0"]
                                                    [integrant "0.8.0"]]
                                     :managed-dependencies [[com.cognitect/transit-clj "1.0.324"]
                                                            [com.google.code.findbugs/jsr305 "3.0.2"]
                                                            [com.google.errorprone/error_prone_annotations "2.1.3"]
                                                            [com.google.guava/guava "25.1-jre"]
                                                            [com.google.protobuf/protobuf-java "3.4.0"]]}

             ;; `dev` in :test is important - a test depends on it:
             :test                  {:source-paths   ["dev"]
                                     :dependencies   [[com.nedap.staffing-solutions/utils.test "1.6.2"]
                                                      [nubank/matcher-combinators "1.0.1"
                                                       :exclusions [commons-codec]]]
                                     :jvm-opts       ["-Dclojure.core.async.go-checking=true"
                                                      "-Duser.language=en-US"]
                                     :resource-paths ["test-resources-extra"
                                                      "test-resources"]}

             :refactor-nrepl        {:dependencies [[refactor-nrepl "2.4.0"]]
                                     ;; exercise cider-nrepl as of those days:
                                     :plugins      [[cider/cider-nrepl "0.22.0"]]}

             ;; There was a 18 month gap between 2.4.0 and 2.5.0, hence the extra build
             :refactor-nrepl-latest {:dependencies [[refactor-nrepl "2.5.0"]]
                                     ;; exercise the latest cider-nrepl, increasingly typical
                                     ;; (and shipped with this refactor-nrepl):
                                     :plugins      [[cider/cider-nrepl "0.24.0"]]}

             :parallel-eastwood     {:jvm-opts ["-Dformatting-stack.eastwood.parallelize-linters=true"]}

             :ncrw                  {:global-vars  {*assert* true} ;; `ci.release-workflow` relies on runtime assertions
                                     :source-paths   ^:replace []
                                     :test-paths     ^:replace []
                                     :resource-paths ^:replace []
                                     :plugins        ^:replace []
                                     :dependencies   ^:replace [[com.nedap.staffing-solutions/ci.release-workflow "1.12.0"]]}

             :ci                    {:pedantic?    :abort
                                     :jvm-opts     ["-Dclojure.main.report=stderr"]}})
