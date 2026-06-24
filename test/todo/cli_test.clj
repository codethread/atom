(ns todo.cli-test
  (:require [clojure.test :refer [deftest is testing]]
            [todo.cli :as cli]
            [todo.db :as db]))

(defn delete-sqlite-family! [db-file]
  (doseq [suffix ["" "-journal" "-wal" "-shm"]]
    (.delete (java.io.File. (str db-file suffix)))))

(defn temp-db-file []
  (let [file (java.io.File/createTempFile "todo-cli-test" ".sqlite")]
    (.delete file)
    (.getAbsolutePath file)))

(defn with-db [f]
  (let [db-file (temp-db-file)
        ds (db/datasource db-file)]
    (try
      (db/init! ds)
      (f ds)
      (finally
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
             [opts command args])))))

(deftest parses-repeatable-command-options
  (is (= {:status "done"
          :attr {:priority "high" :owner "agent"}
          :edge [{:type "depends-on" :to "ue72w"}]}
         (cli/parse-command-options ["--status" "done"
                                     "--attr" "priority=high"
                                     "--attr" "owner=agent"
                                     "--edge" "depends-on:ue72w"]
                                    "summary"))))

(deftest add-and-update-command-cover-core-mutations
  (with-db
    (fn [ds]
      (let [design (cli/run-command! ds "add" ["Design" "--status" "done"] "summary")
            review (cli/run-command! ds "add" ["Review" "--attr" "owner=agent"] "summary")]
        (is (re-matches #"[a-z0-9]+" (:id design)))
        (is (= "done" (:status design)))
        (cli/run-command! ds "update" [(:id review) "--edge" (str "depends-on:" (:id design))] "summary")
        (is (= [(:id review)] (mapv :id (db/ready-tasks ds))))
        (cli/run-command! ds "update" [(:id review) "--status" "done" "--title" "Reviewed"] "summary")
        (let [updated (db/get-task ds (:id review))]
          (is (= "Reviewed" (:title updated)))
          (is (= "done" (:status updated)))
          (is (some? (:final_at updated))))))))

(deftest update-command-rolls-back-on-failure
  (with-db
    (fn [ds]
      (let [target (cli/run-command! ds "add" ["Design"] "summary")
            task (cli/run-command! ds "add" ["Review"] "summary")]
        (is (thrown? Exception
                     (cli/run-command! ds "update" [(:id task) "--edge" "depends-on:missing"] "summary")))
        (is (thrown? Exception
                     (cli/run-command! ds "update" [(:id task) "--edge" (str "depends-on:" (:id target)) "--title" ""] "summary")))
        (is (empty? (db/task-dependencies ds (:id task))))))))
