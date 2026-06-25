(ns todo.cli-test
  (:require [clojure.data.json :as json]
            [clojure.test :refer [deftest is testing]]
            [todo.cli :as cli]
            [todo.client :as client]
            [todo.daemon.metadata :as metadata]
            [todo.daemon.runtime :as runtime]
            [todo.db :as db]))

(defn delete-sqlite-family! [db-file]
  (doseq [suffix ["" "-journal" "-wal" "-shm"]]
    (.delete (java.io.File. (str db-file suffix)))))

(defn temp-db-file []
  (let [file (java.io.File/createTempFile "todo-cli-test" ".sqlite")]
    (.delete file)
    (.getAbsolutePath file)))

(defn with-runtime [f]
  (let [db-file (temp-db-file)
        rt (runtime/start! db-file)]
    (try
      (client/init db-file)
      (f db-file)
      (finally
        (when @runtime/current-runtime
          (runtime/stop! rt))
        (delete-sqlite-family! db-file)))))

(deftest parses-global-options-before-command
  (testing "global options are parsed before the command and command args are preserved"
    (is (= [{:db "/tmp/todo.sqlite" :format "json"} "ready" []]
           (let [[opts command args _summary]
                 (cli/parse-global-options ["--db" "/tmp/todo.sqlite" "--format" "json" "ready"])]
             [opts command args]))))
  (testing "options after the command are command arguments, not global options"
    (is (= [{:db db/default-db-file :format "human"} "ready" ["--format" "json"]]
           (let [[opts command args _summary]
                 (cli/parse-global-options ["ready" "--format" "json"])]
             [opts command args]))))
  (testing "daemon lifecycle commands are parsed after global options"
    (is (= [{:db "/tmp/todo.sqlite" :format "edn"} "daemon" ["status"]]
           (let [[opts command args _summary]
                 (cli/parse-global-options ["--db" "/tmp/todo.sqlite" "--format" "edn" "daemon" "status"])]
             [opts command args])))))

(deftest parses-repeatable-command-options
  (is (= {:status "done"
          :attr {:priority "high" :owner "agent"}
          :edge [{:type "depends-on" :to "ue72w"}]}
         (cli/parse-command-options ["--status" "done"
                                     "--attr" "priority=high"
                                     "--attr" "owner=agent"
                                     "--edge" "depends-on:ue72w"]
                                    "summary")))
  (is (= {:config "daemon.edn"}
         (cli/parse-daemon-start-options ["--config" "daemon.edn"] "summary"))))

(deftest internal-clojure-cli-retains-edn-query-options
  (testing "legacy Clojure CLI parser/client behavior remains available for daemon and REPL support; public Go CLI rejects --where/EDN"
    (with-runtime
    (fn [db-file]
      (let [design (:id (cli/run-command! db-file "add" ["Design" "--status" "done" "--attr" "owner=agent"] "summary"))
            docs (:id (cli/run-command! db-file "add" ["Docs" "--attr" "owner=agent"] "summary"))
            misc (:id (cli/run-command! db-file "add" ["Misc" "--attr" "owner=human"] "summary"))]
        (cli/run-command! db-file "update" [docs "--edge" (str "depends-on:" design)] "summary")
        (client/register-query db-file 'agent-owner '[:= [:attr :owner] "agent"])
        (client/register-query db-file :by-owner '{:params [:owner]
                                                   :where [:= [:attr :owner] [:param :owner]]})
        (is (= #{design docs}
               (set (map :id (cli/run-command! db-file "list" ["--where" "[:= [:attr :owner] \"agent\"]"] "summary")))))
        (is (= #{design docs}
               (set (map :id (cli/run-command! db-file "list" ["--query" "agent-owner"] "summary")))))
        (is (= #{design docs}
               (set (map :id (cli/run-command! db-file "list" ["--query" ":agent-owner"] "summary")))))
        (is (= [docs]
               (mapv :id (cli/run-command! db-file "ready" ["--query" "agent-owner"] "summary"))))
        (is (= [misc]
               (mapv :id (cli/run-command! db-file "list" ["--query" "by-owner" "--param" "owner=human"] "summary")))))))))

(deftest query-options-reject-query-file-and-missing-names
  (with-redefs [cli/fail! (fn [message _summary] (throw (ex-info message {})))]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Unknown option: \"--query-file\""
                          (cli/parse-query-options ["--query" "agent" "--query-file" "queries.edn"] "summary")))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Use either --where or --query, not both"
                          (cli/parse-query-options ["--where" "[:= :status \"todo\"]" "--query" "known"] "summary"))))
  (with-runtime
    (fn [db-file]
      (client/register-query db-file 'known '[:= :status "todo"])
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Query not found: missing .*available: known"
                            (with-redefs [cli/fail! (fn [message _summary] (throw (ex-info message {})))]
                              (cli/-main "--db" db-file "list" "--query" "missing")))))))

(deftest task-commands-reject-daemon-config-option
  (with-redefs [cli/fail! (fn [message _summary] (throw (ex-info message {})))
                client/add (fn [_db-file _request] :unexpected)
                client/update (fn [_db-file _id _request] :unexpected)]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Unknown option: \"--config\""
                          (cli/run-command! "todo.sqlite" "add" ["Task" "--config" "daemon.edn"] "summary")))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Unknown option: \"--config\""
                          (cli/run-command! "todo.sqlite" "update" ["abc12" "--config" "daemon.edn"] "summary")))))

(deftest add-and-update-command-route-through-daemon
  (with-runtime
    (fn [db-file]
      (let [design (cli/run-command! db-file "add" ["Design" "--status" "done"] "summary")
            review (cli/run-command! db-file "add" ["Review" "--attr" "owner=agent"] "summary")]
        (is (re-matches #"[a-z0-9]+" (:id design)))
        (is (= "done" (:status design)))
        (cli/run-command! db-file "update" [(:id review) "--edge" (str "depends-on:" (:id design))] "summary")
        (is (= [(:id review)] (mapv :id (cli/run-command! db-file "ready" [] "summary"))))
        (cli/run-command! db-file "update" [(:id review) "--status" "done" "--title" "Reviewed"] "summary")
        (let [updated (cli/run-command! db-file "show" [(:id review)] "summary")]
          (is (= "Reviewed" (:title updated)))
          (is (= "done" (:status updated)))
          (is (some? (:final_at updated))))))))

(deftest update-command-rolls-back-on-failure
  (with-runtime
    (fn [db-file]
      (let [target (cli/run-command! db-file "add" ["Design" "--status" "done"] "summary")
            task (cli/run-command! db-file "add" ["Review"] "summary")]
        (is (thrown? Exception
                     (cli/run-command! db-file "update" [(:id task) "--edge" "depends-on:missing"] "summary")))
        (is (thrown? Exception
                     (cli/run-command! db-file "update" [(:id task) "--edge" (str "depends-on:" (:id target)) "--title" ""] "summary")))
        (is (some #{(:id task)} (map :id (cli/run-command! db-file "ready" [] "summary"))))))))

(deftest daemon-lifecycle-status-and-failures
  (let [db-file (temp-db-file)]
    (try
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"metadata is missing or stale"
                            (cli/run-command! db-file "list" [] "summary")))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"metadata is missing or stale"
                            (cli/run-command! db-file "add" ["No daemon"] "summary")))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"metadata is missing or stale"
                            (cli/run-command! db-file "show" ["missing"] "summary")))
      (let [rt (runtime/start! db-file)]
        (try
          (client/init db-file)
          (let [status (cli/run-daemon-command! db-file ["status"] "summary")
                edn-status (read-string (with-out-str (cli/-main "--db" db-file "--format" "edn" "daemon" "status")))
                json-status (json/read-str (with-out-str (cli/-main "--db" db-file "--format" "json" "daemon" "status")) :key-fn keyword)]
            (doseq [payload [status edn-status json-status]]
              (is (= "ok" (:health payload)))
              (is (= (metadata/canonical-db-path db-file) (:canonical-db-path payload)))
              (is (integer? (:pid payload)))
              (is (some? (get-in payload [:endpoint :port])))
              (is (some? (get-in payload [:identity :nonce])))))
          (metadata/publish! (assoc (:metadata rt) :nonce "wrong"))
          (is (thrown-with-msg? clojure.lang.ExceptionInfo #"identity does not match"
                                (cli/run-command! db-file "list" [] "summary")))
          (finally
            (metadata/publish! (:metadata rt))
            (runtime/stop! rt))))
      (finally
        (delete-sqlite-family! db-file)))))
