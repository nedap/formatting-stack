(ns formatting-stack.background
  "This file live in a distinct source-paths so it's not affected by the Reloaded workflow,
  while developing formatting-stack itself.")

(defonce workload (atom nil))

(defonce ^Thread
  ^{:doc "The runner for 'background' execution. Can be stopped via the thread interruption mechanism."}
  runner
  (let [f (fn []
            (while (not (-> (Thread/currentThread) .isInterrupted))
              (if-let [job @workload]
                (when (compare-and-set! workload job nil)
                  (try
                    (job)
                    (catch Exception e
                      (-> e .printStackTrace))
                    (catch AssertionError e
                      (-> e .printStackTrace))))
                (Thread/sleep 50))))]
    (-> f
        Thread.
        ;; Important - daemonize this thread, otherwise under certain conditions it can prevent the JVM from exiting.
        ;; (We exercise this implicitly via `lein eastwood` in CI)
        (doto (.setDaemon true))
        (doto .start))))
