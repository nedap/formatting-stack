(ns formatting-stack.global-test-setup
  (:require
   [clojure.tools.namespace.repl :refer [refresh-dirs set-refresh-dirs]]))

(when (System/getenv "CI")
  (-> (reify Thread$UncaughtExceptionHandler
        (uncaughtException [_ thread e]
          (-> e pr-str println)
          (System/exit 1)))
      (Thread/setDefaultUncaughtExceptionHandler)))

;; * the "worker" source-path must be excluded (just like in `dev/dev.clj`)
;; * the "dev" source-path must be excluded (since it's not a concern of the test suite)
;; if updating this, please check if `dev/dev.clj` also needs updating.
(when-not (seq refresh-dirs) ;; Make `lein test` pass (regardless of CI env) while not interfering with dev.clj
  (set-refresh-dirs "src" "test"))
