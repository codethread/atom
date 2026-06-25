(ns todo.repl-test
  (:require [clojure.test :refer [deftest is]]
            [todo.daemon.runtime :as runtime]
            [todo.db-test :as db-test]
            [todo.repl :as repl]))

(defn reset-open-state! []
  (reset! (var-get (ns-resolve 'todo.repl 'active-db-file)) nil))

(defn with-runtime [f]
  (let [db-file (db-test/temp-db-file)
        rt (runtime/start! db-file)]
    (try
      (f rt db-file)
      (finally
        (reset-open-state!)
        (runtime/stop! rt)
        (db-test/delete-sqlite-family! db-file)))))

(deftest helpers-fail-before-open
  (reset-open-state!)
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"No todo daemon is open"
                        (repl/tasks)))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"No todo daemon is open"
                        (repl/load-queries! "/path/does/not/matter.edn"))))

(deftest open-fails-without-selecting-a-daemon
  (let [db-file (db-test/temp-db-file)]
    (try
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"metadata is missing or stale"
                            (repl/open! db-file)))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"No todo daemon is open"
                            (repl/tasks)))
      (finally
        (reset-open-state!)
        (db-test/delete-sqlite-family! db-file)))))

(deftest failed-open-clears-previous-selection
  (with-runtime
    (fn [_ db-file]
      (repl/open! db-file)
      (let [missing-db-file (db-test/temp-db-file)]
        (try
          (is (thrown-with-msg? clojure.lang.ExceptionInfo
                                #"metadata is missing or stale"
                                (repl/open! missing-db-file)))
          (is (thrown-with-msg? clojure.lang.ExceptionInfo
                                #"No todo daemon is open"
                                (repl/tasks)))
          (finally
            (db-test/delete-sqlite-family! missing-db-file)))))))

(deftest dev-user-namespace-loads
  (require 'user :reload)
  (is (some? (ns-resolve 'user 'demo!))))

(deftest helpers-use-daemon-backed-task-flow
  (with-runtime
    (fn [_ db-file]
      (is (= db-file (repl/open! db-file)))
      (is (= {:database "initialized"} (repl/init!)))
      (let [design (repl/task! "Sketch model" "done" {:priority "high"})
            docs (repl/task! "Write docs" {:owner "agent"})]
        (is (= {:priority "high"} (:attributes design)))
        (repl/update! (:id docs) {:edges [{:type "depends-on" :to (:id design)}]})
        (is (= {:owner "agent"} (:attributes (repl/task (:id docs)))))
        (is (= #{(:id design) (:id docs)} (set (map :id (repl/tasks)))))
        (is (= [(:id docs)] (mapv :id (repl/ready))))))))

(deftest query-helpers-use-daemon-backed-task-flow
  (with-runtime
    (fn [_ db-file]
      (repl/open! db-file)
      (repl/init!)
      (let [design (:id (repl/task! "Design" "done" {:owner "agent"}))
            docs (:id (repl/task! "Docs" {:owner "agent"}))
            misc (:id (repl/task! "Misc" {:owner "human"}))]
        (repl/update! docs {:edges [{:type "depends-on" :to design}]})
        (is (= {"agent-ready" {:params [:owner]
                                :where [:= [:attr :owner] [:param :owner]]}}
               (repl/defquery! 'agent-ready {:params [:owner]
                                             :where [:= [:attr :owner] [:param :owner]]})))
        (is (= {"agent-ready" {:params [:owner]
                                :where [:= [:attr :owner] [:param :owner]]}}
               (repl/queries)))
        (is (= #{design docs}
               (set (map :id (repl/tasks 'agent-ready {:owner "agent"})))))
        (is (= [docs]
               (mapv :id (repl/ready [:= [:attr :owner] "agent"]))))
        (is (= [docs]
               (mapv :id (repl/ready :agent-ready {:owner "agent"}))))
        (is (= [misc]
               (mapv :id (repl/query :agent-ready {:owner "human"}))))))))

(deftest query-registry-helpers-use-daemon-memory
  (with-runtime
    (fn [rt db-file]
      (repl/open! db-file)
      (repl/init!)
      (let [agent (:id (repl/task! "Agent task" {:owner "agent"}))
            human (:id (repl/task! "Human task" {:owner "human"}))]
        (is (= {"mine" [:= [:attr :owner] "agent"]}
               (repl/defquery! :mine [:= [:attr :owner] "agent"])))
        (is (= {"mine" [:= [:attr :owner] "agent"]}
               (repl/queries)))
        (is (= [agent] (mapv :id (repl/tasks 'mine))))
        (runtime/stop! rt)
        (let [fresh-rt (runtime/start! db-file)]
          (try
            (is (= {} (repl/queries)))
            (is (thrown-with-msg? clojure.lang.ExceptionInfo
                                  #"Query not found"
                                  (repl/tasks :mine)))
            (let [query-file (java.io.File/createTempFile "todo-queries" ".edn")]
              (try
                (spit query-file (pr-str {'mine [:= [:attr :owner] "human"]}))
                (is (= {"mine" [:= [:attr :owner] "human"]}
                       (repl/load-queries! (.getAbsolutePath query-file))))
                (is (= [human] (mapv :id (repl/query :mine))))
                (spit query-file "{mine [:= [:attr :owner] \"agent\"]} {:extra true}")
                (is (thrown-with-msg? clojure.lang.ExceptionInfo
                                      #"exactly one form"
                                      (repl/load-queries! (.getAbsolutePath query-file))))
                (finally
                  (.delete query-file))))
            (finally
              (runtime/stop! fresh-rt))))))))

(deftest helpers-fail-loudly-when-daemon-becomes-unavailable
  (let [db-file (db-test/temp-db-file)
        rt (runtime/start! db-file)]
    (try
      (repl/open! db-file)
      (runtime/stop! rt)
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"metadata is missing or stale"
                            (repl/tasks)))
      (finally
        (reset-open-state!)
        (runtime/stop! rt)
        (db-test/delete-sqlite-family! db-file)))))
