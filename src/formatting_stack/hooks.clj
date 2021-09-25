(ns formatting-stack.hooks
  (:require
   [formatting-stack.plugin :as plugin]
   [formatting-stack.report :as report]
   [nedap.speced.def :as speced]))

(speced/defn run-hook [^var? hook
                       ^ifn? acc-fn
                       ^coll? plugin-chain
                       arg]
  (cond
    (= #'plugin/cli-options hook)
    (->> plugin-chain
         (filter (set (keys (methods @hook))))
         (reduce (fn [acc id] (acc-fn acc (hook id))) arg))

    :else
    (->> plugin-chain
         (filter (set (keys (methods @hook))))
         (reduce (fn [acc id]
                   ;; FIXME rename config to context
                   (report/report {:type :hook-start
                                   :hook hook
                                   :plugin id
                                   :config arg})
                   (let [r (acc-fn acc (hook id arg))]
                     (report/report {:type :hook-end
                                     :hook hook
                                     :plugin id
                                     :config r})
                     r))
                 arg))))
