(ns todo.smoke
  (:require [clojure.data.json :as json]
            [todo.db :as db]
            [todo.repl :as repl]))

(def smoke-db "smoke.sqlite")
(def cli-smoke-db "smoke-cli.sqlite")

(def seed-tasks
  [{:id "design" :title "Sketch task graph model" :attributes {:priority "high" :due-date "2026-07-01" :status "done"}}
   {:id "schema" :title "Create SQLite schema" :attributes {:priority "high" :due-date "2026-07-02" :status "done"}}
   {:id "tui" :title "Build terminal UI" :attributes {:priority "medium" :due-date "2026-07-05" :status "doing"}}
   {:id "docs" :title "Write usage notes" :attributes {:priority "low" :due-date "2026-07-08" :status "todo"}}
   {:id "release" :title "Package MVP" :attributes {:priority "high" :due-date "2026-07-10" :status "todo" :estimate-hours 2}}])

(def seed-edges
  [{:from "schema" :to "design" :type "depends-on" :attributes {:reason "schema follows model"}}
   {:from "tui" :to "schema" :type "depends-on" :attributes {:reason "TUI persists tasks"}}
   {:from "docs" :to "tui" :type "depends-on" :attributes {:reason "document real commands"}}
   {:from "release" :to "docs" :type "depends-on" :attributes {:reason "ship docs"}}
   {:from "release" :to "tui" :type "depends-on" :attributes {:reason "ship UI"}}
   {:from "docs" :to "design" :type "mentions" :attributes {:section "architecture"}}])

(defn section [title rows]
  (println "\n--" title "--")
  (doseq [row rows] (println row)))

(defn ids [rows]
  (mapv :id rows))

(defn delete-sqlite-family! [db-file]
  (doseq [suffix ["" "-journal" "-wal" "-shm"]]
    (.delete (java.io.File. (str db-file suffix)))))

(defn run-cli! [db-file & args]
  (let [command (into ["clojure" "-M:todo" "--db" db-file] args)
        process (-> (ProcessBuilder. command)
                    (.redirectErrorStream true)
                    (.start))
        output (slurp (.getInputStream process))
        exit-code (.waitFor process)]
    (assert (= 0 exit-code)
            (str "CLI command succeeds: " (pr-str command) "\n" output))
    output))

(defn assert= [expected actual message]
  (assert (= expected actual)
          (str message "\nexpected: " (pr-str expected) "\nactual: " (pr-str actual))))

(defn -main [& [db-file]]
  (let [ds (db/datasource (or db-file smoke-db))
        cli-db (if db-file (str db-file ".cli") cli-smoke-db)]
    (delete-sqlite-family! cli-db)
    (run-cli! cli-db "init")
    (run-cli! cli-db "add" "design" "Sketch task graph model" "--attr" "priority=high" "--attr" "due-date=2026-07-01" "--attr" "status=done")
    (run-cli! cli-db "add" "schema" "Create SQLite schema" "--attr" "priority=high" "--attr" "due-date=2026-07-02" "--attr" "status=todo")
    (run-cli! cli-db "add" "docs" "Write usage notes" "--attr" "priority=low" "--attr" "due-date=2026-07-08" "--attr" "status=todo" "--attr" "owner=agent")
    (run-cli! cli-db "link" "schema" "design" "depends-on" "--attr" "reason=schema follows model")
    (run-cli! cli-db "link" "docs" "schema" "depends-on" "--attr" "reason=document real commands")
    (assert= ["schema"] (ids (read-string (run-cli! cli-db "--format" "edn" "ready"))) "CLI process ready sees tasks with done dependencies")
    (assert= ["docs"] (ids (json/read-str (run-cli! cli-db "--format" "json" "by-attr" "owner" "agent") :key-fn keyword)) "CLI process by-attr queries JSON1 task attributes")
    (assert= ["design"] (ids (read-string (run-cli! cli-db "--format" "edn" "deps" "schema"))) "CLI process deps queries graph relationships")
    (assert= ["design" "schema"] (ids (read-string (run-cli! cli-db "--format" "edn" "transitive-deps" "docs"))) "CLI process transitive-deps traverses graph relationships")
    (run-cli! cli-db "done" "schema")
    (assert= ["docs"] (ids (read-string (run-cli! cli-db "--format" "edn" "ready"))) "CLI process done updates status used by ready")
    (section "agent CLI process ready" (read-string (run-cli! cli-db "--format" "edn" "ready")))
    (section "agent CLI process by-attr owner=agent" (json/read-str (run-cli! cli-db "--format" "json" "by-attr" "owner" "agent") :key-fn keyword))
    (db/reset-db! ds)
    (doseq [task seed-tasks]
      (db/add-task! ds task))
    (doseq [edge seed-edges]
      (db/add-edge! ds edge))
    (assert= "design" (:id (db/get-task ds "design")) "get-task retrieves one seeded task")
    (db/update-task-attributes! ds "docs" {:owner "agent"})
    (assert= {:priority "low" :due-date "2026-07-08" :status "todo" :owner "agent"}
             (db/<-json (:attributes (db/get-task ds "docs")))
             "update-task-attributes! patches JSON attributes")
    (assert= ["tui"] (ids (db/ready-tasks ds)) "only tui is ready before tui is done")
    (db/update-task-status! ds "tui" "done")
    (assert= ["docs"] (ids (db/ready-tasks ds)) "docs becomes ready after tui is done")
    (assert= ["release"] (ids (db/blocking-tasks ds "docs")) "release directly depends on docs")
    (assert= ["design" "docs" "schema" "tui"] (ids (db/transitive-dependencies ds "release")) "release transitive dependencies")
    (assert= ["release"] (ids (db/tasks-by-attribute ds :estimate-hours 2)) "arbitrary JSON attribute lookup")
    (section "all tasks" (db/all-tasks ds))
    (section "high priority tasks via JSON1" (db/tasks-by-priority ds "high"))
    (section "tasks due by 2026-07-05 via JSON1" (db/tasks-due-before ds "2026-07-05"))
    (section "blocked tasks from edge table" (db/blocked-tasks ds))
    (section "release dependencies" (db/task-dependencies ds "release"))
    (section "release transitive dependencies" (db/transitive-dependencies ds "release"))
    (section "tasks blocked by docs" (db/blocking-tasks ds "docs"))
    (section "estimate-hours=2 via arbitrary JSON attribute" (db/tasks-by-attribute ds :estimate-hours 2))
    (section "docs graph edges" (db/related-tasks ds "docs"))
    (let [repl-db (str (or db-file smoke-db) ".repl")
          expected-repl-helpers '#{open! init! task! depends! edge! done! tasks task deps transitive-deps blocking ready by-attr graph}]
      (assert= expected-repl-helpers (set (keys (select-keys (ns-publics 'todo.repl) expected-repl-helpers))) "todo.repl exposes the MVP helper vocabulary")
      (try
        (repl/ready)
        (throw (ex-info "Expected todo.repl helpers to fail before open!" {}))
        (catch clojure.lang.ExceptionInfo e
          (assert (re-find #"No todo database is open" (.getMessage e)))))
      (.delete (java.io.File. repl-db))
      (repl/open! repl-db)
      (repl/init!)
      (repl/task! "a" "First task" {:status "done"})
      (repl/task! "b" "Second task" {:status "todo"})
      (repl/depends! "b" "a")
      (assert= ["b"] (ids (repl/ready)) "todo.repl ready returns tasks with done dependencies")
      (repl/done! "b")
      (assert= "done" (:status (:attributes (repl/task "b"))) "todo.repl done! updates status"))
    (println "\nSmoke database:" (or db-file smoke-db))))
