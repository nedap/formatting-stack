(ns formatting-stack.background
  (:import
   (java.util.concurrent LinkedBlockingQueue ThreadPoolExecutor ThreadPoolExecutor$DiscardOldestPolicy TimeUnit)))

(defonce workload (atom nil))

;; executor which drops > 1 items in the queue
(defonce executor
  (ThreadPoolExecutor.
   1 1 0 TimeUnit/MILLISECONDS
   (LinkedBlockingQueue. 1)
   (ThreadPoolExecutor$DiscardOldestPolicy.)))

(add-watch workload ::scheduler
           (fn [_ _ _ current]
             (when (fn? current)
               (.execute executor current))))
