(ns skein.config-dashboard-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [skein.db-test :as db-test]
            [skein.libs.alpha :as libs]
            [skein.views.alpha :as views]
            [skein.weaver.api :as api]
            [skein.weaver.config :as daemon-config]
            [skein.weaver.runtime :as runtime]))

(defn- test-world
  "Return an isolated test world rooted in a temporary directory."
  [config-dir]
  (daemon-config/world config-dir
                       (str config-dir "/state")
                       (str config-dir "/data")))

(defn- with-config-runtime
  "Run f with an isolated runtime and the repo-local .skein config loaded."
  [f]
  (let [db-file (db-test/temp-db-file)
        config-dir (str "/tmp/skein-config-dashboard-" (java.util.UUID/randomUUID))]
    (.mkdirs (java.io.File. config-dir))
    (let [rt (runtime/start! db-file {:world (test-world config-dir)})]
      (try
        (load-file ".skein/config.clj")
        ((requiring-resolve 'config/install!))
        (f rt)
        (finally
          (runtime/stop! rt)
          (db-test/delete-sqlite-family! db-file))))))

(defn- copy-config-dir!
  "Copy the repo-local config files into a temporary config dir."
  [target]
  (.mkdirs (io/file target))
  (doseq [name ["init.clj" "config.clj" "libs.edn"]]
    (io/copy (io/file ".skein" name) (io/file target name))))

(defn- with-startup-config-runtime
  "Run f with an isolated runtime started through copied .skein/init.clj."
  [f]
  (let [db-file (db-test/temp-db-file)
        config-dir (str "/tmp/skein-config-startup-" (java.util.UUID/randomUUID))]
    (copy-config-dir! config-dir)
    (let [rt (runtime/start! db-file {:world (test-world config-dir)})]
      (try
        (f rt)
        (finally
          (runtime/stop! rt)
          (db-test/delete-sqlite-family! db-file))))))

(defn- add-devflow!
  "Create an active devflow strand for dashboard tests."
  [rt title kind feature attrs edges]
  (api/add rt (cond-> {:title title
                       :state "active"
                       :attributes (merge {:workflow "devflow"
                                           :kind kind
                                           :feature feature}
                                          attrs)}
                (seq edges) (assoc :edges edges))))

(defn- assert-dashboard-registrations
  "Assert repo-local devflow query/view/op registrations are present."
  [rt]
  (is (contains? (api/queries rt) "devflow-work"))
  (is (contains? (api/queries rt) "devflow-features"))
  (is (some #(= "devflow-dashboard" (:name %)) (views/views)))
  (is (some #(= :devflow-coordination-attrs (:key %)) (api/hooks rt)))
  (is (some #(= "devflow-status" (:name %)) (api/ops rt))))

(deftest devflow-coordination-hook-normalizes-task-id-and-validates-present-attrs
  (with-config-runtime
    (fn [rt]
      (let [created (api/add rt {:title "Numeric task id"
                                 :state "active"
                                 :attributes {:workflow "devflow"
                                              :kind "task"
                                              :feature "normalize"
                                              :task_key "yqztl"
                                              :task_id 42
                                              :validation ["clojure -M:test"]}})
            socket-shaped (api/add rt {:title "Socket numeric task id"
                                       :state "active"
                                       :attributes {"workflow" "devflow"
                                                    "kind" "task"
                                                    "feature" "normalize"
                                                    "task_key" "socket"
                                                    "task_id" 43
                                                    "validation" ["clojure -M:test"]}})]
        (is (= "42" (get-in created [:attributes :task_id])))
        (is (= "43" (get-in socket-shaped [:attributes :task_id]))))
      (try
        (api/add rt {:title "Bad task id"
                     :state "active"
                     :attributes {:workflow "devflow"
                                  :kind "task"
                                  :feature "normalize"
                                  :task_key "bad"
                                  :task_id ""}})
        (is false "expected malformed task_id rejection")
        (catch clojure.lang.ExceptionInfo e
          (is (= "hook/failed" (:code (ex-data e))))
          (is (= :attributes/normalize (:hook/type (ex-data e))))
          (is (= :devflow-coordination-attrs (:hook/key (ex-data e))))
          (is (= "devflow/invalid-coordination-attribute" (:hook/cause-code (ex-data e))))))
      (try
        (api/add rt {:title "Bad string task id"
                     :state "active"
                     :attributes {"workflow" "devflow"
                                  "kind" "task"
                                  "feature" "normalize"
                                  "task_key" "bad-string"
                                  "task_id" ""}})
        (is false "expected malformed string-keyed task_id rejection")
        (catch clojure.lang.ExceptionInfo e
          (is (= "hook/failed" (:code (ex-data e))))
          (is (= "devflow/invalid-coordination-attribute" (:hook/cause-code (ex-data e))))))
      (try
        (api/add rt {:title "Bad validation"
                     :state "active"
                     :attributes {:workflow "devflow"
                                  :kind "task"
                                  :feature "normalize"
                                  :task_key "bad-validation"
                                  :validation []}})
        (is false "expected empty validation rejection")
        (catch clojure.lang.ExceptionInfo e
          (is (= "hook/failed" (:code (ex-data e))))
          (is (= "devflow/invalid-coordination-attribute" (:hook/cause-code (ex-data e))))))
      (doseq [[title attrs] [["Duplicate task id" {:workflow "devflow"
                                                    :kind "task"
                                                    :feature "normalize"
                                                    :task_key "dupe-task-id"
                                                    :task_id 1
                                                    "task_id" 2}]
                            ["Duplicate workflow" {:workflow "devflow"
                                                    "workflow" "agent-plan"
                                                    :kind "task"
                                                    :feature "normalize"
                                                    :task_key "dupe-workflow"}]]]
        (try
          (api/add rt {:title title
                       :state "active"
                       :attributes attrs})
          (is false "expected duplicate logical coordination key rejection")
          (catch clojure.lang.ExceptionInfo e
            (is (= "hook/failed" (:code (ex-data e))))
            (is (= "devflow/duplicate-coordination-attribute" (:hook/cause-code (ex-data e)))))))
      (is (thrown? clojure.lang.ExceptionInfo
                   (api/weave! rt :devflow-plan {:feature "normalize"
                                                 :title "Plan"
                                                 :tasks [{:key "empty-validation"
                                                          :title "Empty validation"
                                                          :validation []}]}))))))

(deftest devflow-status-unscoped-ready-excludes-plan-strands
  (with-config-runtime
    (fn [rt]
      (add-devflow! rt "Feature" "plan" "dashboard-review" {} [])
      (add-devflow! rt "Ready task" "task" "dashboard-review" {:task_key "ready"} [])
      (let [status ((requiring-resolve 'config/devflow-status-op) {:op/argv []})]
        (is (= 2 (:active_count status)))
        (is (= 1 (:ready_count status)))
        (is (= ["Ready task"] (mapv :title (:ready status))))))))

(deftest devflow-dashboard-includes-active-blockers-outside-feature-candidate-set
  (with-config-runtime
    (fn [rt]
      (add-devflow! rt "Feature" "plan" "dashboard-review" {} [])
      (let [external-blocker (api/add rt {:title "External blocker"
                                          :state "active"
                                          :attributes {:kind "task"
                                                       :feature "other-feature"
                                                       :task_key "external"
                                                       :owner "outside"}})]
        (add-devflow! rt "Blocked work" "task" "dashboard-review"
                      {:task_key "blocked"
                       :task_file "devflow/feat/dashboard/tasks/blocked.md"
                       :owner "agent"}
                      [{:type "depends-on" :to (:id external-blocker)}])
        (let [dashboard (views/view! 'devflow-dashboard {:feature "dashboard-review"})
              blocked (get-in dashboard [:features 0 :blocked 0])]
          (is (= {:features 1 :active_work 1 :ready_work 0 :blocked_work 1}
                 (:counts dashboard)))
          (is (= "Blocked work" (:title blocked)))
          (is (= [{:id (:id external-blocker)
                   :title "External blocker"
                   :edge_type "depends-on"
                   :metadata {:feature "other-feature"
                              :kind "task"
                              :task_key "external"
                              :owner "outside"}
                   :attributes {}}]
                 (:blocked_by blocked))))))))

(deftest repo-local-startup-and-reload-preserve-dashboard-registrations
  (with-startup-config-runtime
    (fn [rt]
      (assert-dashboard-registrations rt)
      (add-devflow! rt "Feature" "plan" "startup-dashboard" {} [])
      (let [external-blocker (api/add rt {:title "External blocker"
                                          :state "active"
                                          :attributes {:kind "task"
                                                       :feature "external"
                                                       :task_key "external"}})]
        (add-devflow! rt "Ready work" "task" "startup-dashboard"
                      {:task_key "ready"
                       :task_file "devflow/feat/startup/tasks/ready.md"
                       :owner "agent"}
                      [])
        (add-devflow! rt "Blocked work" "review" "startup-dashboard"
                      {:task_key "blocked"
                       :task_file "devflow/feat/startup/tasks/blocked.md"
                       :owner "reviewer"}
                      [{:type "depends-on" :to (:id external-blocker)}])
        (let [before-status ((requiring-resolve 'config/devflow-status-op) {:op/argv ["startup-dashboard"]})
              before-dashboard (views/view! 'devflow-dashboard {:feature "startup-dashboard"})]
          (is (= 3 (:active_count before-status)))
          (is (= 1 (:ready_count before-status)))
          (is (= {:features 1 :active_work 2 :ready_work 1 :blocked_work 1}
                 (:counts before-dashboard)))
          (is (= "External blocker" (get-in before-dashboard [:features 0 :blocked 0 :blocked_by 0 :title]))))
        (is (= :loaded (:status (libs/reload!))))
        (assert-dashboard-registrations rt)
        (let [after-status ((requiring-resolve 'config/devflow-status-op) {:op/argv ["startup-dashboard"]})
              after-dashboard (views/view! 'devflow-dashboard {:feature "startup-dashboard"})]
          (is (= 3 (:active_count after-status)))
          (is (= 1 (:ready_count after-status)))
          (is (= ["Ready work"] (mapv :title (:ready after-status))))
          (is (= {:features 1 :active_work 2 :ready_work 1 :blocked_work 1}
                 (:counts after-dashboard)))
          (is (= "External blocker" (get-in after-dashboard [:features 0 :blocked 0 :blocked_by 0 :title]))))))))
