(ns formatting-stack.background)

(defonce workload (atom nil))

(defonce runner
  (future
    (while (not (-> (Thread/currentThread) .isInterrupted))
      (if-let [job @workload]
        (when (compare-and-set! workload job nil)
          (try
            (job)
            (catch Throwable e
              (-> e .printStackTrace))))
        (Thread/sleep 50)))))
