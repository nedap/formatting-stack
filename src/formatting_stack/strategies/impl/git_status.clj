(ns formatting-stack.strategies.impl.git-status
  "Functions for `git status` parsing. Note that commands such as `git diff`, `git ls-files` do not share its output format."
  (:require
   [clojure.string :as string]
   [nedap.speced.def :as speced]))

(def deletion-markers
  ;; NOTE: order matters (there are tests covering this), particularly for the last two members.
  ["DD "
   "DM "
   "DU "
   "AD "
   "CD "
   "MD "
   "RD "
   "UD "
   " D "
   "D "])

(speced/defn deleted-file? [^string? s]
  (->> deletion-markers
       (some (fn [marker]
               (string/starts-with? s marker)))
       (boolean)))

(speced/defn remove-deletion-markers [^string? s]
  (->> deletion-markers
       (reduce (fn [result marker]
                 (string/replace-first result marker ""))
               s)))
