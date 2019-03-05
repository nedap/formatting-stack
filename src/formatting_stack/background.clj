(ns formatting-stack.background)

(def workload (atom nil))

(defonce runner
  (future
    (while (not (-> (Thread/currentThread) .isInterrupted))
      (if-let [job @workload]
        (do
          (compare-and-set! workload job nil)
          (job))
        (Thread/sleep 50)))))

(comment ;; perform the following before `refresh`ing this ns:
  (future-cancel runner))
