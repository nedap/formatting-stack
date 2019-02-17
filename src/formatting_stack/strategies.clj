(ns formatting-stack.strategies
  (:require
   [clojure.string :as str]
   [formatting-stack.strategies.impl :as impl]))

(defn all-files
  "This strategy unconditionally processes all files."
  []
  (let [tracked (impl/file-entries "git" "ls-files")
        untracked (impl/file-entries "git" "ls-files" "--others" "--exclude-standard")]
    (-> untracked (into tracked) impl/extract-clj-files)))

(defn git-completely-staged
  "This strategy processes the new or modified files that are _completely_ staged with git."
  [& {:keys [files]
      :or {files (impl/file-entries "git" "status" "--porcelain")}}]
  (->> files
       (filter #(re-find impl/git-completely-staged-regex %))
       (map #(str/replace-first % impl/git-completely-staged-regex ""))
       impl/extract-clj-files))

(defn git-not-completely-staged
  "This strategy processes all files that are not _completely_ staged with git. Untracked files are also included."
  [& {:keys [files]
      :or {files (impl/file-entries "git" "status" "--porcelain")}}]
  (->> files
       (filter #(re-find impl/git-not-completely-staged-regex %))
       (map #(str/replace-first % impl/git-not-completely-staged-regex ""))
       impl/extract-clj-files))

(defn git-diff-against-default-branch
  "This strategy processes all files that this branch has modified.
  The diff is compared against the `:target-branch` option."
  [& {:keys [target-branch files blacklist]
      :or {target-branch "master"
           files (impl/file-entries "git" "diff" "--name-only" target-branch)
           blacklist (git-not-completely-staged)}}]
  (->> files
       (remove (set blacklist))
       impl/extract-clj-files))

(defn do-not-use-cached-results!
  "Normally, subsequent 'members' (formatters, linters, compilers)
  using identical strategies will cache the results of those strategies.
  That is apt for formatters that do safe modifications, but not for more dangerous formatters.

  By adding this empty strategy, it is signaled that the member using it should not use a cached result.

  You can find a detailed explanation/example in https://git.io/fh7E0 ."
  [])
