(ns todo.smoke
  (:require [todo.db :as db]))

(def smoke-db "smoke.sqlite")

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

(defn -main [& [db-file]]
  (let [ds (db/datasource (or db-file smoke-db))]
    (db/reset-db! ds)
    (doseq [task seed-tasks]
      (db/add-task! ds task))
    (doseq [edge seed-edges]
      (db/add-edge! ds edge))
    (section "all tasks" (db/all-tasks ds))
    (section "high priority tasks via JSON1" (db/tasks-by-priority ds "high"))
    (section "tasks due by 2026-07-05 via JSON1" (db/tasks-due-before ds "2026-07-05"))
    (section "blocked tasks from edge table" (db/blocked-tasks ds))
    (section "release dependencies" (db/task-dependencies ds "release"))
    (section "docs graph edges" (db/related-tasks ds "docs"))
    (println "\nSmoke database:" (or db-file smoke-db))))
