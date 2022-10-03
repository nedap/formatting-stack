(def eastwood-version "1.2.3")

;; Please don't bump the library version by hand - use ci.release-workflow instead.
(defproject formatting-stack "4.6.0"
  ;; Please keep the dependencies sorted a-z.
  :dependencies [[clj-kondo "2022.02.09"]
                 [cljfmt "0.8.0"]
                 [com.gfredericks/how-to-ns "0.2.9"]
                 [com.gfredericks/lein-all-my-files-should-end-with-exactly-one-newline-character "0.1.2"]
                 [com.nedap.staffing-solutions/speced.def "2.1.1"]
                 [com.nedap.staffing-solutions/utils.collections "2.2.1"]
                 [com.nedap.staffing-solutions/utils.modular "2.2.0"]
                 [com.nedap.staffing-solutions/utils.spec.predicates "1.2.1"]
                 [io.reflectoring.diffparser/diffparser "1.4"]
                 [jonase/eastwood ~eastwood-version]
                 [medley "1.3.0"]
                 [org.clojure/clojure "1.10.3"]
                 [org.clojure/java.classpath "1.0.0"]
                 [org.clojure/java.data "1.0.95"]
                 [org.clojure/tools.namespace "1.2.0"]
                 [org.clojure/tools.reader "1.3.6"]]

  :managed-dependencies [[rewrite-clj "1.0.699-alpha"]
                         [org.slf4j/slf4j-api "1.7.30"]]

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

  :plugins [[lein-pprint "1.3.2"]
            [jonase/eastwood ~eastwood-version]]

  ;; A variety of common dependencies are bundled with `nedap/lein-template`.
  ;; They are divided into two categories:
  ;; * Dependencies that are possible or likely to be needed in all kind of production projects
  ;;   * The point is that when you realise you needed them, they are already in your classpath, avoiding interrupting your flow
  ;;   * After realising this, please move the dependency up to the top level.
  ;; * Genuinely dev-only dependencies allowing 'basic science'
  ;;   * e.g. criterium, deep-diff, clj-java-decompiler

  ;; Manage transitive deps using :managed-dependencies, see https://git.io/JtUGI
  :profiles {:dev                   {:dependencies [[com.clojure-goes-fast/clj-java-decompiler "0.3.1"]
                                                    [com.stuartsierra/component.repl "1.0.0"]
                                                    [com.taoensso/timbre "4.10.0"]
                                                    [criterium "0.4.6"]
                                                    [integrant/repl "0.3.2"]
                                                    [lambdaisland/deep-diff "0.0-47"]
                                                    [org.slf4j/slf4j-nop "1.7.30"]
                                                    [org.clojure/core.async "1.5.648"]
                                                    [org.clojure/math.combinatorics "0.1.6"]
                                                    [org.clojure/test.check "1.1.1"]]
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

             :cljs-old              {:dependencies [[org.clojure/clojurescript
                                                     #_"Please do not change, its entire point is to exercise an old version in CI"
                                                     "1.7.228"]]}

             :provided              {:dependencies         [[com.stuartsierra/component "0.4.0"]
                                                            [integrant "0.8.0"]
                                                            [org.clojure/clojurescript "1.11.4"]]
                                     :managed-dependencies [[cheshire "5.10.2"]
                                                            [com.cognitect/transit-clj "1.0.329"]
                                                            [com.google.code.findbugs/jsr305 "3.0.2"]
                                                            [com.google.errorprone/error_prone_annotations "2.11.0"]
                                                            [com.google.guava/guava "31.1-jre"]
                                                            [com.google.javascript/closure-compiler-unshaded "v20220202"]
                                                            [com.google.protobuf/protobuf-java "3.19.4"]]}

             ;; `dev` in :test is important - a test depends on it:
             :test                  {:source-paths   ["dev"]
                                     :dependencies   [[com.nedap.staffing-solutions/utils.test "1.6.2"]
                                                      [nubank/matcher-combinators "1.0.1"]]
                                     :managed-dependencies [[commons-codec "1.11"]]
                                     :jvm-opts       ["-Dclojure.core.async.go-checking=true"
                                                      "-Duser.language=en-US"]
                                     :resource-paths ["test-resources-extra"
                                                      "test-resources"]}

             :refactor-nrepl        {:dependencies [[refactor-nrepl "3.5.2"]
                                                    [nrepl "0.9.0"]]
                                     ;; cider-nrepl is a :provided dependency from refactor-nrepl.
                                     :plugins      [[cider/cider-nrepl "0.28.2"
                                                     ;; not excluding nrepl will cause conflicting versions
                                                     :exclusions [nrepl]]]}

             :ncrw                  {:global-vars    {*assert* true} ;; `ci.release-workflow` relies on runtime assertions
                                     :source-paths   ^:replace []
                                     :test-paths     ^:replace []
                                     :resource-paths ^:replace []
                                     :plugins        ^:replace []
                                     :dependencies   ^:replace [[com.nedap.staffing-solutions/ci.release-workflow "1.13.1"]]}

             :ci                    {:pedantic? :abort
                                     :jvm-opts  ["-Dclojure.main.report=stderr"]}})
