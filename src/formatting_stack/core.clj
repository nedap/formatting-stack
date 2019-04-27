(ns formatting-stack.core
  (:require
   [clojure.main]
   [formatting-stack.background]
   [formatting-stack.defaults :refer :all]
   [formatting-stack.indent-specs :refer [default-third-party-indent-specs]]
   [formatting-stack.protocols.compiler :as protocols.compiler]
   [formatting-stack.protocols.formatter :as protocols.formatter]
   [formatting-stack.protocols.linter :as protocols.linter]
   [formatting-stack.util :refer [with-serialized-output]]))

(defn files-from-strategies [strategies]
  (->> strategies
       (reduce (fn [files strategy]
                 (strategy :files files))
               [])
       distinct))

(def newliner
  (reify
    protocols.formatter/Formatter
    (format! [_ _]
      (println))

    protocols.linter/Linter
    (lint! [_ _]
      (println))

    protocols.compiler/Compiler
    (compile! [_ _]
      (println))))

(defn process! [method members category-strategies default-strategies intersperse-newlines?]
  ;; `memoize` rationale: results are cached not for performance,
  ;; but for avoiding the scenario where one `member` alters the git status,
  ;; so the subsequent `member`s' strategies won't perceive the same set of files than the first one.
  ;; e.g. cljfmt may operate upon `strategies/git-completely-staged`, formatting some files accordingly.
  ;; Then `how-to-ns`, which follows the same strategy, would perceive a dirty git status.
  ;; Accordingly it would do nothing, which is undesirable.
  (let [members (if-not intersperse-newlines?
                  members
                  (->> members (interpose newliner)))
        files (memoize (fn [strategies]
                         (files-from-strategies strategies)))]
    (with-serialized-output
      (doseq [member members]
        (let [{specific-strategies :strategies} member
              strategies (or specific-strategies category-strategies default-strategies)]
          (try
            (->> strategies files (method member))
            (catch Exception e
              (println "Encountered an exception, which will be printed in the next line."
                       "formatting-stack execution has *not* been aborted.")
              (-> e .printStackTrace))))))))

(defn format! [& {:keys [strategies
                         third-party-indent-specs
                         formatters
                         linters
                         compilers
                         in-background?
                         intersperse-newlines?]}]
  ;; the following `or` clauses ensure that Components don't pass nil values
  (let [strategies               (or strategies default-strategies)
        third-party-indent-specs (or third-party-indent-specs default-third-party-indent-specs)
        formatters               (or formatters (default-formatters third-party-indent-specs))
        linters                  (or linters default-linters)
        compilers                (or compilers default-compilers)
        in-background?           (if (some? in-background?)
                                   in-background?
                                   true)
        {formatters-strategies
         :formatters
         linters-strategies
         :linters
         compilers-strategies
         :compilers
         default-strategies
         :default}               strategies
        impl (bound-fn [] ;; important that it's a bound-fn (for an undetermined reason)
               (process! protocols.formatter/format! formatters formatters-strategies strategies intersperse-newlines?)
               (when intersperse-newlines?
                 (println))
               (process! protocols.linter/lint!      linters    linters-strategies    strategies intersperse-newlines?)
               (when intersperse-newlines?
                 (println))
               (process! protocols.compiler/compile! compilers  compilers-strategies  strategies intersperse-newlines?))]
    (if in-background?
      (do
        (reset! formatting-stack.background/workload impl)
        'Running...)
      (do
        (impl)
        'Done))))
