(ns formatting-stack.integrant
  (:require
   [formatting-stack.core :refer [format!]]
   [integrant.core :as integrant]))

(defmethod integrant/init-key ::component [_ _]
  (fn [_]
    (future ;; don't delay system initialization
      (format!))))

(defmethod integrant/halt-key! ::component [_ _]
  (fn [_]))
