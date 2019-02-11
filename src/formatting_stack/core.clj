(ns formatting-stack.core
  (:require
   [formatting-stack.formatters.cider]
   [formatting-stack.formatters.cljfmt]
   [formatting-stack.formatters.how-to-ns]
   [formatting-stack.indent-specs :refer [default-third-party-indent-specs]]
   [formatting-stack.protocols.compiler :as protocols.compiler]
   [formatting-stack.protocols.formatter :as protocols.formatter]
   [formatting-stack.protocols.linter :as protocols.linter]
   [formatting-stack.strategies :as strategies]))

(def default-strategies [strategies/git-completely-staged])

(defn default-formatters [third-party-indent-specs]
  (let [opts {:third-party-indent-specs third-party-indent-specs}]
    [(formatting-stack.formatters.cider/map->Formatter opts)
     (formatting-stack.formatters.cljfmt/map->Formatter opts)
     (formatting-stack.formatters.how-to-ns/map->Formatter opts)]))

(def default-linters [])

(def default-compilers [])

(defn format! [& {:keys [strategies
                         third-party-indent-specs
                         formatters
                         linters
                         compilers]}]
  ;; the following `or` clauses ensure that Components don't pass nil values
  (let [strategies               (or strategies default-strategies)
        third-party-indent-specs (or third-party-indent-specs default-third-party-indent-specs)
        formatters               (or formatters (default-formatters third-party-indent-specs))
        linters                  (or linters default-linters)
        compilers                (or compilers default-compilers)
        files                    (->> strategies
                                      (mapcat (fn [f]
                                                (f)))
                                      distinct)]
    (doseq [formatter formatters]
      (protocols.formatter/format! formatter files))
    (doseq [linter linters]
      (protocols.linter/lint! linter files))
    (doseq [compiler compilers]
      (protocols.compiler/compile! compiler files))))
