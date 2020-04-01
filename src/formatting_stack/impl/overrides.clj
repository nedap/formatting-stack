(ns formatting-stack.impl.overrides
  (:require
   [clojure.spec.alpha :as spec]
   [formatting-stack.protocols.spec :as protocols.spec]
   [formatting-stack.util :refer [rcomp]]
   [nedap.speced.def :as speced]))

(defn removing-deep-merge
  "A deep-merge in which `nil` values express that the given key-value entry should be removed."
  ([a]
   (if-not (map? a)
     a
     (-> (into {}
               (filter (rcomp val some?))
               a)
         (with-meta (meta a)))))

  ([a b]
   (let [v (if (and (map? a) (map? b))
             (merge-with removing-deep-merge a b)
             b)]
     (if (map? v)
       (removing-deep-merge v)
       v))))

(spec/def ::overrides (spec/map-of (spec/and :formatting-stack.protocols.spec.member/id
                                             (fn [x]
                                               (-> x name #{"id"})))
                                   (spec/or :removal  nil?
                                            :override map?
                                            :reify    ::protocols.spec/reify)))

(speced/defn apply-overrides [^::protocols.spec/members members, ^::overrides overrides]
  (let [reify-ids (->> overrides
                       (filter (rcomp val (partial spec/valid? ::protocols.spec/reify)))
                       (map key)
                       (set))]
    (->> overrides
         (reduce (fn [result [member-id member-overrides]]
                   (if (reify-ids member-id)
                     (conj result member-overrides)
                     (let [index (->> result
                                      (map-indexed (fn [i m]
                                                     (when (-> m
                                                               (get :id)
                                                               #{member-id})
                                                       i)))
                                      (keep identity)
                                      ;; 0 or 1 items will be returned - that is covered by the `::protocols.spec/members` spec:
                                      (first))]
                       (assert index (str "Member not found: " (pr-str member-id) " in " (pr-str result)))
                       (update result index removing-deep-merge member-overrides))))
                 members)
         ;; clean up removals:
         (remove nil?)
         (vec))))
