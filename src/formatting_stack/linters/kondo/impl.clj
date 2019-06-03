(ns formatting-stack.linters.kondo.impl
  "Vendorized/forked copy of clj-kondo.main. https://github.com/borkdude/clj-kondo#license"
  (:require
   [clj-kondo.impl.analyzer :as ana]
   [clj-kondo.impl.cache :as cache]
   [clj-kondo.impl.config :as config]
   [clj-kondo.impl.linters :as l]
   [clj-kondo.impl.namespace :as namespace]
   [clj-kondo.impl.overrides :refer [overrides]]
   [clj-kondo.impl.profiler :as profiler]
   [clj-kondo.impl.state :as state]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str :refer [ends-with? starts-with?]])
  (:import
   (java.util.jar JarFile JarFile$JarFileEntry)))

(def dev? (= "true" (System/getenv "CLJ_KONDO_DEV")))

(def ^:private version (str/trim
                        (slurp (io/resource "CLJ_KONDO_VERSION"))))
(set! *warn-on-reflection* true)

(defn- format-output []
  (if-let [^String pattern (-> @config/config :output :pattern)]
    (fn [filename row col level message]
      (-> pattern
          (str/replace "{{filename}}" filename)
          (str/replace "{{row}}" (str row))
          (str/replace "{{col}}" (str col))
          (str/replace "{{level}}" (name level))
          (str/replace "{{LEVEL}}" (str/upper-case (name level)))
          (str/replace "{{message}}" message)))
    (fn [filename row col level message]
      (str filename ":" row ":" col ": " (name level) ": " message))))

(defn- print-findings [findings]
  (let [format-fn (format-output)]
    (doseq [{:keys [filename message level row col] :as finding}
            (dedupe (sort-by (juxt :filename :row :col) findings))]
      (println (format-fn filename row col level message)))))

(defn- print-version []
  (println (str "clj-kondo v" version)))

(defn- print-help []
  (print-version)
  ;; TODO: document config format when stable enough
  (println (format "
Usage: [ --help ] [ --version ] [ --lint <files> ] [ --lang (clj|cljs) ] [ --cache [ <dir> ] ] [ --config <config> ]

Options:

  --lint: a file can either be a normal file, directory or classpath. In the
    case of a directory or classpath, only .clj, .cljs and .cljc will be
    processed. Use - as filename for reading from stdin.

  --lang: if lang cannot be derived from the file extension this option will be
    used.

  --cache: if dir exists it is used to write and read data from, to enrich
    analysis over multiple runs. If no value is provided, the nearest .clj-kondo
    parent directory is detected and a cache directory will be created in it.

  --config: config may be a file or an EDN expression. See
    https://cljdoc.org/d/clj-kondo/clj-kondo/%s/doc/configuration.
" version))
  nil)

(defn- source-file? [filename]
  (or (ends-with? filename ".clj")
      (ends-with? filename ".cljc")
      (ends-with? filename ".cljs")))

(defn- sources-from-jar
  [^String jar-path]
  (let [jar (JarFile. jar-path)
        entries (enumeration-seq (.entries jar))
        entries (filter (fn [^JarFile$JarFileEntry x]
                          (let [nm (.getName x)]
                            (source-file? nm))) entries)]
    (map (fn [^JarFile$JarFileEntry entry]
           {:filename (.getName entry)
            :source   (slurp (.getInputStream jar entry))}) entries)))

(defn- sources-from-dir
  [dir]
  (let [files (file-seq dir)]
    (keep (fn [^java.io.File file]
            (let [nm (.getPath file)
                  can-read? (.canRead file)
                  source? (source-file? nm)]
              (cond
                (and can-read? source?)
                {:filename nm
                 :source   (slurp file)}
                (and (not can-read?) source?)
                (do (println (str nm ":0:0:") "warning: can't read, check file permissions")
                    nil)
                :else nil)))
          files)))

(defn- lang-from-file [file default-language]
  (cond (ends-with? file ".clj")
        :clj
        (ends-with? file ".cljc")
        :cljc
        (ends-with? file ".cljs")
        :cljs
        :else default-language))

(def ^:private cp-sep (System/getProperty "path.separator"))

(defn- classpath? [f]
  (str/includes? f cp-sep))

(defn- process-file [filename default-language]
  (try
    (let [file (io/file filename)]
      (cond
        (.exists file)
        (if (.isFile file)
          (if (ends-with? file ".jar")
            ;; process jar file
            (mapcat #(ana/analyze-input (:filename %) (:source %)
                                        (lang-from-file (:filename %) default-language)
                                        dev?)
                    (sources-from-jar filename))
            ;; assume normal source file
            (ana/analyze-input filename (slurp filename)
                               (lang-from-file filename default-language)
                               dev?))
          ;; assume directory
          (mapcat #(ana/analyze-input (:filename %) (:source %)
                                      (lang-from-file (:filename %) default-language)
                                      dev?)
                  (sources-from-dir file)))
        (= "-" filename)
        (ana/analyze-input "<stdin>" (slurp *in*) default-language dev?)
        (classpath? filename)
        (mapcat #(process-file % default-language)
                (str/split filename
                           (re-pattern cp-sep)))
        :else
        [{:findings [{:level    :warning
                      :filename filename
                      :col      0
                      :row      0
                      :message  "file does not exist"}]}]))
    (catch Throwable e
      (if dev? (throw e)
          [{:findings [{:level    :warning
                        :filename filename
                        :col      0
                        :row      0
                        :message  "could not process file"}]}]))))

(defn- process-files [files default-lang]
  (mapcat #(process-file % default-lang) files))

;;;; find cache/config dir

(defn- config-dir
  ([] (config-dir
       (io/file
        (System/getProperty "user.dir"))))
  ([cwd]
   (loop [dir (io/file cwd)]
     (let [cfg-dir (io/file dir ".clj-kondo")]
       (if (.exists cfg-dir)
         (if (.isDirectory cfg-dir)
           cfg-dir
           (throw (Exception. (str cfg-dir " must be a directory"))))
         (when-let [parent (.getParentFile dir)]
           (recur parent)))))))

(def ^:private
  empty-cache-opt-warning
  "WARNING: --cache option didn't specify directory, but no .clj-kondo directory found. Continuing without cache. See https://github.com/borkdude/clj-kondo/blob/master/README.md#project-setup.")

(defn parse-opts [options]
  (let [opts (loop [options options
                    opts-map {}
                    current-opt nil]
               (if-let [opt (first options)]
                 (if (starts-with? opt "--")
                   (recur (rest options)
                          (assoc opts-map opt [])
                          opt)
                   (recur (rest options)
                          (update opts-map current-opt conj opt)
                          current-opt))
                 opts-map))
        default-lang (case (first (get opts "--lang"))
                       "clj"  :clj
                       "cljs" :cljs
                       "cljc" :cljc
                       :clj)
        cache-opt (get opts "--cache")
        cfg-dir (config-dir)
        cache-dir (when cache-opt
                    (if-let [cd (first cache-opt)]
                      (io/file cd version)
                      (if cfg-dir (io/file cfg-dir ".cache" version)
                          (do (println empty-cache-opt-warning)
                              nil))))
        files (get opts "--lint")
        raw-config (first (get opts "--config"))
        config-edn? (when raw-config
                      (str/starts-with? raw-config "{"))
        config-opt (and raw-config
                        (if config-edn?
                          (edn/read-string raw-config)
                          (edn/read-string (slurp raw-config))))
        config-edn (when cfg-dir
                     (let [f (io/file cfg-dir "config.edn")]
                       (when (.exists f)
                         (edn/read-string (slurp f)))))]
    {:opts         opts
     :files        files
     :cache-dir    cache-dir
     :default-lang default-lang
     :configs      [config-edn config-opt]}))

(defn- mmerge
  "Merges maps no deeper than two levels"
  [a b]
  (merge-with merge a b))

(defn- index-defs-and-calls [defs-and-calls]
  (reduce
   (fn [acc {:keys [:calls :defs :used :lang] :as m}]
     (-> acc
         (update-in [lang :calls] (fn [prev-calls]
                                    (merge-with into prev-calls calls)))
         (update-in [lang :defs] mmerge defs)
         (update-in [lang :used] into used)))
   {:clj  {:calls {} :defs {} :used #{}}
    :cljs {:calls {} :defs {} :used #{}}
    :cljc {:calls {} :defs {} :used #{}}}
   defs-and-calls))

(def ^:private zinc (fnil inc 0))

(defn- summarize [findings]
  (reduce (fn [acc {:keys [:level]}]
            (update acc level zinc))
          {:error 0 :warning 0 :info 0}
          findings))

(defn- filter-findings [findings]
  (let [print-debug? (:debug @config/config)
        filter-output (not-empty (-> @config/config :output :include-files))
        remove-output (not-empty (-> @config/config :output :exclude-files))]
    (for [{:keys [:filename :level :type] :as f} findings
          :let [level (or (when type (-> @config/config :linters type :level))
                          level)]
          :when (and level (not= :off level))
          :when (if (= :debug type)
                  print-debug?
                  true)
          :when (if filter-output
                  (some (fn [pattern]
                          (re-find (re-pattern pattern) filename))
                        filter-output)
                  true)
          :when (not-any? (fn [pattern]
                            (re-find (re-pattern pattern) filename))
                          remove-output)]
      (assoc f :level level))))

(defn lint! [{:keys [opts files default-lang cache-dir configs]}]
  (state/clear-findings!)
  (reset! namespace/namespaces {})
  (run! config/merge-config! configs)
  (let [processed (process-files files default-lang)
        idacs (-> processed
                  index-defs-and-calls
                  (cache/sync-cache cache-dir)
                  overrides)
        linted-calls (doall (l/lint-calls idacs))
        _ (l/lint-unused-namespaces!)
        all-findings (concat linted-calls (mapcat :findings processed)
                             @state/findings)
        all-findings (filter-findings all-findings)
        {:keys [error warning]} (summarize all-findings)]
    (when (-> @config/config :output :show-progress)
      (println))
    (print-findings all-findings)
    (cond (pos? error)   3
          (pos? warning) 2
          :else          0)))
