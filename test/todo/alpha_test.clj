(ns todo.alpha-test
  (:require [atom.graph.alpha :as graph]
            [atom.views.alpha :as views]
            [clojure.test :refer [deftest is]]
            [todo.client]
            [todo.daemon.api :as api]
            [todo.daemon.config :as daemon-config]
            [todo.daemon.runtime :as runtime]
            [todo.db-test :as db-test]
            [todo.repl :as repl]))

(defn reset-repl-state! []
  (reset! (var-get (ns-resolve 'todo.repl 'active-config-dir))
          (var-get (ns-resolve 'todo.repl 'no-connection))))

(defn with-runtime [f]
  (let [db-file (db-test/temp-db-file)
        config-dir (str "/tmp/atom-alpha-" (java.util.UUID/randomUUID))]
    (.mkdirs (java.io.File. config-dir))
    (let [rt (runtime/start! db-file {:world (daemon-config/world config-dir)})]
      (try
        (f rt)
        (finally
          (reset-repl-state!)
          (runtime/stop! rt)
          (db-test/delete-sqlite-family! db-file))))))

(defn test-view [{:keys [params]}]
  {:alpha-view params})

(deftest alpha-helpers-route-directly-inside-daemon-runtime
  (with-runtime
    (fn [rt]
      (api/init rt)
      (let [feature (api/add rt {:title "Feature" :attributes {:kind "feature"}})
            task (api/add rt {:title "Task" :attributes {:owner "agent"}})]
        (api/update rt (:id feature) {:edges [{:type "parent-of" :to (:id task)}]})
        (api/register-query rt 'agent-owned [:= [:attr :owner] "agent"])
        (is (= [(:id task)] (graph/query-ids! 'agent-owned {})))
        (is (= [(:id task)] (mapv :id (graph/tasks-by-ids [(:id task) (:id task)]))))
        (is (= [(:id feature)] (graph/ancestor-root-ids [(:id task)] {})))
        (is (= #{(:id feature) (:id task)}
               (set (map :id (:tasks (graph/subgraph [(:id feature)]))))))
        (is (= {:name "daily" :fn 'todo.alpha-test/test-view}
               (views/register-view! :daily 'todo.alpha-test/test-view)))
        (is (= [{:name "daily" :fn 'todo.alpha-test/test-view}]
               (views/views)))
        (is (= {:alpha-view {:owner "agent"}}
               (views/view! 'daily {:owner "agent"})))))))

(deftest alpha-helpers-route-through-connected-helper-context
  (with-redefs [runtime/current-runtime (atom nil)
                repl/connected-config-dir (constantly "/tmp/atom-connected-world")
                todo.client/call-world (fn [config-dir opts op & args]
                                         {:config-dir config-dir
                                          :opts opts
                                          :op op
                                          :args args})]
    (is (= {:config-dir "/tmp/atom-connected-world"
            :opts {}
            :op :query-ids
            :args ['mine {:owner "agent"}]}
           (graph/query-ids! 'mine {:owner "agent"})))
    (is (= {:config-dir "/tmp/atom-connected-world"
            :opts {}
            :op :tasks-by-ids
            :args [["a" "b"]]}
           (graph/tasks-by-ids ["a" "b"])))
    (is (= {:config-dir "/tmp/atom-connected-world"
            :opts {}
            :op :ancestor-root-ids
            :args [["leaf"] {:where [:= [:attr :kind] "feature"]}]}
           (graph/ancestor-root-ids ["leaf"] {:where [:= [:attr :kind] "feature"]})))
    (is (= {:config-dir "/tmp/atom-connected-world"
            :opts {}
            :op :subgraph
            :args [["root"]]}
           (graph/subgraph ["root"])))
    (is (= {:config-dir "/tmp/atom-connected-world"
            :opts {}
            :op :register-view!
            :args ['daily 'my.views/daily]}
           (views/register-view! 'daily 'my.views/daily)))
    (is (= {:config-dir "/tmp/atom-connected-world"
            :opts {}
            :op :view!
            :args ['daily {:owner "agent"}]}
           (views/view! 'daily {:owner "agent"})))
    (is (= {:config-dir "/tmp/atom-connected-world"
            :opts {}
            :op :views
            :args nil}
           (views/views)))))

