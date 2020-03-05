(ns formatting-stack.strategies.impl.git-diff
  "Functions for `git diff --name-status` parsing.

  Note that commands such as `git status`, `git ls-files` do not share its output format."
  (:require
   [clojure.string :as string]
   [nedap.speced.def :as speced]))

(speced/defn deletion? [^string? s]
  (-> s (string/starts-with? "D\t")))

;; See: `git diff --help`, `diff-filter` section
(speced/defn remove-markers [^string? s]
 (-> s (string/replace-first #"^(A|C|D|M|R|T|U|X|B)\t" "")))
