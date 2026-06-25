(ns todo.libs-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [atom.libs.alpha :as libs]
            [todo.daemon.config :as daemon-config]
            [todo.daemon.runtime :as runtime]
            [todo.db-test :as db-test]
            [todo.repl :as repl]))

(defn- temp-config-dir []
  (doto (.toFile (java.nio.file.Files/createTempDirectory
                  (.toPath (io/file "/tmp"))
                  "atom-libs-config"
                  (make-array java.nio.file.attribute.FileAttribute 0)))
    (.mkdirs)))

(defn- delete-recursive [file]
  (doseq [child (reverse (file-seq file))]
    (.delete child)))

(defn- with-runtime [f]
  (let [db-file (db-test/temp-db-file)
        config-dir (temp-config-dir)]
    (try
      (let [rt (runtime/start! db-file {:world (daemon-config/world (.getCanonicalPath config-dir))})]
        (try
          (f rt config-dir)
          (finally
            (runtime/stop! rt))))
      (finally
        (db-test/delete-sqlite-family! db-file)
        ;; Runtime-added local roots are retained for the process lifetime by tools.deps.
        ;; Keep temp config dirs so later add-libs calls do not see stale basis entries.
        nil))))

(defn- write-libs! [config-dir content]
  (spit (io/file config-dir "libs.edn") content))

(defn- write-local-lib! [config-dir lib-name ns-sym]
  (let [root (io/file config-dir "libs" lib-name)
        ns-path (-> (str ns-sym)
                    (.replace \- \_)
                    (.replace \. java.io.File/separatorChar))
        src-file (io/file root "src" (str ns-path ".clj"))]
    (.mkdirs (.getParentFile src-file))
    (spit src-file (str "(ns " ns-sym ")\n(defn marker [] :synced-lib-loaded)\n"))
    (spit (io/file root "deps.edn") "{:paths [\"src\"]}\n")
    root))

(deftest approved-returns-empty-libs-when-file-is-missing
  (with-runtime
    (fn [_ _]
      (is (= {:libs {}} (libs/approved))))))

(deftest approved-fails-when-libs-edn-is-not-a-file
  (with-runtime
    (fn [_ config-dir]
      (.mkdirs (io/file config-dir "libs.edn"))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"malformed or unreadable"
                            (libs/approved))))))

(deftest approved-normalizes-relative-and-absolute-roots
  (with-runtime
    (fn [_ config-dir]
      (let [relative-root (io/file config-dir "libs" "demo")
            absolute-root (io/file config-dir "external" "abs")]
        (.mkdirs relative-root)
        (.mkdirs absolute-root)
        (write-libs! config-dir
                     (pr-str {:libs {'demo/relative {:local/root "libs/demo"}
                                     'demo/absolute {:local/root (.getAbsolutePath absolute-root)}}}))
        (is (= {:libs {'demo/absolute {:local/root (.getAbsolutePath absolute-root)
                                       :root (.getCanonicalPath absolute-root)}
                       'demo/relative {:local/root "libs/demo"
                                       :root (.getCanonicalPath relative-root)}}}
               (libs/approved)))))))

(deftest approved-canonicalizes-symlink-roots
  (with-runtime
    (fn [_ config-dir]
      (let [target (io/file config-dir "libs" "target")
            link (io/file config-dir "libs" "link")]
        (.mkdirs target)
        (java.nio.file.Files/createSymbolicLink (.toPath link) (.toPath target)
                                                (make-array java.nio.file.attribute.FileAttribute 0))
        (write-libs! config-dir (pr-str {:libs {'demo/link {:local/root "libs/link"}}}))
        (is (= {:libs {'demo/link {:local/root "libs/link"
                                   :root (.getCanonicalPath target)}}}
               (libs/approved)))))))

(deftest approved-does-not-reject-missing-local-roots
  (with-runtime
    (fn [_ config-dir]
      (let [missing (io/file config-dir "libs" "missing")]
        (write-libs! config-dir (pr-str {:libs {'demo/missing {:local/root "libs/missing"}}}))
        (is (= {:libs {'demo/missing {:local/root "libs/missing"
                                      :root (.getCanonicalPath missing)}}}
               (libs/approved)))))))

(deftest approved-routes-through-connected-helper-context
  (with-redefs [runtime/current-runtime (atom nil)
                repl/connected-config-dir (constantly "/tmp/atom-connected-world")
                todo.client/call-world (fn [config-dir opts op & args]
                                         {:config-dir config-dir
                                          :opts opts
                                          :op op
                                          :args args})]
    (is (= {:config-dir "/tmp/atom-connected-world"
            :opts {}
            :op :approved-libs
            :args nil}
           (libs/approved)))
    (is (= {:config-dir "/tmp/atom-connected-world"
            :opts {}
            :op :sync-approved-libs
            :args nil}
           (libs/sync!)))
    (is (= {:config-dir "/tmp/atom-connected-world"
            :opts {}
            :op :approved-lib-syncs
            :args nil}
           (libs/syncs)))))

(deftest approved-fails-loudly-on-structural-errors
  (with-runtime
    (fn [_ config-dir]
      (doseq [[label content pattern]
              [["malformed EDN" "{:libs" #"malformed or unreadable"]
               ["unknown top-level key" (pr-str {:libs {} :extra true}) #"unknown top-level keys"]
               ["missing :libs" (pr-str {}) #"requires :libs map"]
               ["non-map :libs" (pr-str {:libs []}) #"requires :libs map"]
               ["non-symbol coordinate" (pr-str {:libs {"demo/lib" {:local/root "libs/demo"}}}) #"coordinate must be a symbol"]
               ["non-map entry" (pr-str {:libs {'demo/lib "libs/demo"}}) #"entry must be a map"]
               ["unknown per-lib key" (pr-str {:libs {'demo/lib {:local/root "libs/demo" :extra true}}}) #"unknown keys"]
               ["missing root" (pr-str {:libs {'demo/lib {}}}) #"requires non-blank string"]
               ["non-string root" (pr-str {:libs {'demo/lib {:local/root 1}}}) #"requires non-blank string"]
               ["blank root" (pr-str {:libs {'demo/lib {:local/root "  "}}}) #"requires non-blank string"]]]
        (testing label
          (write-libs! config-dir content)
          (is (thrown-with-msg? clojure.lang.ExceptionInfo pattern (libs/approved))))))))

(deftest sync-loads-approved-local-root-and-exposes-state
  (with-runtime
    (fn [_ config-dir]
      (let [suffix (.replace (str (java.util.UUID/randomUUID)) "-" "")
            ns-sym (symbol (str "demo.synced-" suffix))
            lib (symbol (str "demo/lib-" suffix))
            root (write-local-lib! config-dir "demo" ns-sym)]
        (write-libs! config-dir (pr-str {:libs {lib {:local/root "libs/demo"}}}))
        (is (= {:libs {lib {:lib lib
                            :local/root "libs/demo"
                            :root (.getCanonicalPath root)
                            :status :loaded}}}
               (libs/sync!)))
        (is (= {:libs {lib {:lib lib
                            :local/root "libs/demo"
                            :root (.getCanonicalPath root)
                            :status :loaded}}}
               (libs/syncs)))
        (is (= {:libs {lib {:lib lib
                            :local/root "libs/demo"
                            :root (.getCanonicalPath root)
                            :status :already-available}}}
               (libs/sync!)))))))

(deftest daemon-init-runs-with-library-classloader-after-sync
  (let [db-file (db-test/temp-db-file)
        config-dir (temp-config-dir)
        suffix (.replace (str (java.util.UUID/randomUUID)) "-" "")
        ns-sym (symbol (str "demo.init-synced-" suffix))
        lib (symbol (str "demo/init-lib-" suffix))
        result-file (io/file config-dir "init-result.edn")]
    (write-local-lib! config-dir "init-demo" ns-sym)
    (try
      (write-libs! config-dir (pr-str {:libs {lib {:local/root "libs/init-demo"}}}))
      (spit (io/file config-dir "init.clj")
            (str "(do\n"
                 "  (require '[atom.libs.alpha :as libs])\n"
                 "  (libs/sync!)\n"
                 "  (require '" ns-sym ")\n"
                 "  (spit " (pr-str (str result-file))
                 " (pr-str ((requiring-resolve '" (symbol (str ns-sym "/marker")) ")))))\n"))
      (let [rt (runtime/start! db-file {:world (daemon-config/world (.getCanonicalPath config-dir))})]
        (try
          (is (= :synced-lib-loaded (read-string (slurp result-file))))
          (is (= :loaded (get-in (libs/syncs) [:libs lib :status])))
          (finally
            (runtime/stop! rt))))
      (finally
        (db-test/delete-sqlite-family! db-file)
        (when-not (.exists result-file)
          (delete-recursive config-dir))))))

(deftest sync-clears-stale-state-before-structural-failure
  (with-runtime
    (fn [_ config-dir]
      (let [missing (io/file config-dir "libs" "missing")]
        (write-libs! config-dir (pr-str {:libs {'demo/missing {:local/root "libs/missing"}}}))
        (is (= {:libs {'demo/missing {:lib 'demo/missing
                                      :local/root "libs/missing"
                                      :root (.getCanonicalPath missing)
                                      :status :failed
                                      :reason :missing-root}}}
               (libs/sync!)))
        (write-libs! config-dir "{:libs")
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"malformed or unreadable" (libs/sync!)))
        (is (= {:libs {}} (libs/syncs)))))))

(deftest sync-records-runtime-add-failures-as-failed-outcomes
  (with-runtime
    (fn [_ config-dir]
      (let [root (io/file config-dir "libs" "bad-deps")]
        (.mkdirs root)
        (spit (io/file root "deps.edn") "{:paths [}")
        (write-libs! config-dir (pr-str {:libs {'demo/bad-deps {:local/root "libs/bad-deps"}}}))
        (let [result (get-in (libs/sync!) [:libs 'demo/bad-deps])]
          (is (= {:lib 'demo/bad-deps
                  :local/root "libs/bad-deps"
                  :root (.getCanonicalPath root)
                  :status :failed
                  :reason :runtime-add-failed}
                 (select-keys result [:lib :local/root :root :status :reason])))
          (is (string? (:message result)))
          (is (string? (:class result))))))))

(deftest sync-records-missing-and-unreadable-roots-as-failed-outcomes
  (with-runtime
    (fn [_ config-dir]
      (let [not-dir (io/file config-dir "libs" "not-dir")]
        (.mkdirs (.getParentFile not-dir))
        (spit not-dir "not a directory")
        (write-libs! config-dir (pr-str {:libs {'demo/missing {:local/root "libs/missing"}
                                                'demo/not-dir {:local/root "libs/not-dir"}}}))
        (is (= {:libs {'demo/missing {:lib 'demo/missing
                                      :local/root "libs/missing"
                                      :root (.getCanonicalPath (io/file config-dir "libs" "missing"))
                                      :status :failed
                                      :reason :missing-root}
                       'demo/not-dir {:lib 'demo/not-dir
                                      :local/root "libs/not-dir"
                                      :root (.getCanonicalPath not-dir)
                                      :status :failed
                                      :reason :unreadable-root}}}
               (libs/sync!)))))))
