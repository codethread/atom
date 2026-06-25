(ns user
  (:require [todo.daemon.runtime :as runtime]
            [todo.repl :refer :all]))

(def demo-db "/tmp/todo-demo.sqlite")
(defonce ^:private demo-runtime (atom nil))

(defn start-demo-daemon!
  "Start the demo daemon explicitly. Call before demo!/seed-demo!, or start an equivalent daemon from the CLI."
  []
  (when @demo-runtime
    (throw (ex-info "Demo daemon is already started from this REPL" {:database demo-db})))
  (reset! demo-runtime (runtime/start! demo-db))
  {:database demo-db
   :status :daemon-started})

(defn stop-demo-daemon!
  "Stop the demo daemon started by start-demo-daemon!."
  []
  (let [rt (or @demo-runtime
               (throw (ex-info "No demo daemon was started from this REPL" {:database demo-db})))]
    (runtime/stop! rt)
    (reset! demo-runtime nil)
    {:database demo-db
     :status :daemon-stopped}))

(defn demo!
  "Connect to an already-running demo daemon and initialize the database."
  []
  (connect!)
  (init!)
  {:database demo-db
   :status :ready})

(defn seed-demo!
  "Initialize the demo database and add a small dependency graph."
  []
  (demo!)
  (let [design (task! "Sketch model" "done" {:priority "high" :demo-id "design"})
        docs (task! "Write docs" {:owner "agent" :demo-id "docs"})
        impl (task! "Build feature" {:owner "agent" :demo-id "impl"})]
    (update! (:id docs) {:edges [{:type "depends-on" :to (:id design)}]})
    (update! (:id impl) {:edges [{:type "depends-on" :to (:id docs)}]})
    (tasks)))

(comment
  (start-demo-daemon!)
  (demo!)
  (seed-demo!)
  (ready)
  (def docs-id (:id (first (filter #(= "docs" (get-in % [:attributes :demo-id])) (tasks)))))
  (update! docs-id {:status "done"})
  (ready)
  (stop-demo-daemon!))
