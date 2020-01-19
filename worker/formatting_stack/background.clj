(ns formatting-stack.background
  "This file live in a distinct source-paths so it's not affected by the Reloaded workflow,
  while developing formatting-stack itself.")

(defonce workload (atom nil))

(defonce runner
  (future
    (while (not (-> (Thread/currentThread) .isInterrupted))
      (if-let [job @workload]
        (when (compare-and-set! workload job nil)
          (try
            (job)
            (catch Exception e
              (-> e .printStackTrace))
            (catch AssertionError e
              (-> e .printStackTrace))))
        (Thread/sleep 50)))))
