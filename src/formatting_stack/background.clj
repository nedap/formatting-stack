(ns formatting-stack.background)

(defonce workload (atom nil))

(defonce runner
  (future
    (while (not (-> (Thread/currentThread) .isInterrupted))
      (if-let [job @workload]
        (when (compare-and-set! workload job nil)
          (try
            (job)
            (catch Exception e
              (-> e .printStackTrace))))
        (Thread/sleep 50)))))

(comment ;; perform the following before `refresh`ing this ns:
  (future-cancel runner))
