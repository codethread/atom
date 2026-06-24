(ns todo.db-test
  (:require [clojure.test :refer [deftest is testing]]
            [todo.db :as db]))

(defn delete-sqlite-family! [db-file]
  (doseq [suffix ["" "-journal" "-wal" "-shm"]]
    (.delete (java.io.File. (str db-file suffix)))))

(defn temp-db-file []
  (let [file (java.io.File/createTempFile "todo-db-test" ".sqlite")]
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

(deftest task-creation-and-attribute-validation
  (with-db
    (fn [ds]
      (testing "tasks are created with generated ids and open-ended JSON attributes"
        (let [task (-> (db/add-task! ds {:title "Sketch model"
                                         :attributes {:priority "high"}})
                       (update :attributes db/<-json))]
          (is (re-matches #"[a-z0-9]+" (:id task)))
          (is (= {:title "Sketch model"
                  :status "todo"
                  :attributes {:priority "high"}
                  :final_at nil}
                 (select-keys task [:title :status :attributes :final_at])))))
      (testing "generated task ids are unique across task creation"
        (let [first-task (db/add-task! ds {:title "First" :attributes {}})
              second-task (db/add-task! ds {:title "Second" :attributes {}})]
          (is (not= (:id first-task) (:id second-task)))))
      (testing "invalid task inputs fail before SQLite writes"
        (is (thrown-with-msg? clojure.lang.ExceptionInfo
                              #"Invalid task"
                              (db/add-task! ds {:title "" :attributes {}})))
        (is (thrown-with-msg? clojure.lang.ExceptionInfo
                              #"Invalid task"
                              (db/add-task! ds {:title "Bad attrs"
                                                :attributes [:nope]})))))))

(deftest dependency-readiness-semantics
  (with-db
    (fn [ds]
      (let [design (:id (db/add-task! ds {:title "Design" :status "done"}))
            schema (:id (db/add-task! ds {:title "Schema"}))
            docs (:id (db/add-task! ds {:title "Docs"}))]
        (db/add-edge! ds {:from schema
                          :to design
                          :type "depends-on"
                          :attributes {:reason "follows design"}})
        (db/add-edge! ds {:from docs :to schema :type "depends-on" :attributes {}})

        (testing "ready tasks are incomplete tasks with all direct dependencies done"
          (is (= [schema] (mapv :id (db/ready-tasks ds))))
          (db/update-task-status! ds schema "done")
          (is (= [docs] (mapv :id (db/ready-tasks ds)))))))))

(deftest graph-queries-follow-depends-on-direction
  (with-db
    (fn [ds]
      (let [design (:id (db/add-task! ds {:title "Design" :attributes {}}))
            schema (:id (db/add-task! ds {:title "Schema" :attributes {}}))
            docs (:id (db/add-task! ds {:title "Docs" :attributes {}}))]
        (db/add-edge! ds {:from schema :to design :type "depends-on" :attributes {}})
        (db/add-edge! ds {:from docs :to schema :type "depends-on" :attributes {}})

        (is (= [design] (mapv :id (db/task-dependencies ds schema))))
        (is (= [schema] (mapv :id (db/blocking-tasks ds design))))
        (is (= #{design schema} (set (mapv :id (db/transitive-dependencies ds docs)))))))))

(deftest edge-schema-validation
  (with-db
    (fn [ds]
      (let [a (:id (db/add-task! ds {:title "A" :attributes {}}))
            b (:id (db/add-task! ds {:title "B" :attributes {}}))
            c (:id (db/add-task! ds {:title "C" :attributes {}}))]
        (testing "only canonical edge types are accepted"
          (is (some? (db/add-edge! ds {:from a :to b :type "related-to" :attributes {}})))
          (is (some? (db/add-edge! ds {:from a :to c :type "parent-of" :attributes {}})))
          (is (some? (db/add-edge! ds {:from b :to c :type "supersedes" :attributes {}})))
          (is (thrown-with-msg? clojure.lang.ExceptionInfo
                                #"Invalid edge"
                                (db/add-edge! ds {:from a :to b :type "mentions" :attributes {}}))))
        (testing "edges must keep the task graph acyclic"
          (is (thrown-with-msg? clojure.lang.ExceptionInfo
                                #"same task"
                                (db/add-edge! ds {:from a :to a :type "related-to" :attributes {}})))
          (is (thrown-with-msg? clojure.lang.ExceptionInfo
                                #"create a cycle"
                                (db/add-edge! ds {:from c :to a :type "related-to" :attributes {}}))))))))

(deftest batch-creation-resolves-refs-and-existing-ids
  (with-db
    (fn [ds]
      (let [existing (:id (db/add-task! ds {:title "Existing" :attributes {}}))
            result (db/add-task-batch!
                    ds
                    [{:ref 'design
                      :title "Design"
                      :status "done"}
                     {:ref 'docs
                      :title "Docs"
                      :edges [{:type "depends-on" :to 'design}
                              {:type "related-to" :to existing :attributes {:kind "prior"}}]}])
            refs (:refs result)
            docs-id (get refs "docs")]
        (is (= #{"design" "docs"} (set (keys refs))))
        (is (= 2 (count (:created result))))
        (is (= [(get refs "design")] (mapv :id (db/task-dependencies ds docs-id))))
        (is (= #{"depends-on" "related-to"}
               (set (map :edge_type (db/related-tasks ds docs-id)))))))))

(deftest batch-creation-preserves-namespaced-ref-identity
  (with-db
    (fn [ds]
      (let [result (db/add-task-batch!
                    ds
                    [{:ref 'foo/design :title "Foo design"}
                     {:ref 'bar/design :title "Bar design"}
                     {:ref 'docs
                      :title "Docs"
                      :edges [{:type "depends-on" :to 'foo/design}]}])
            refs (:refs result)]
        (is (= #{"foo/design" "bar/design" "docs"} (set (keys refs))))
        (is (= [(get refs "foo/design")]
               (mapv :id (db/task-dependencies ds (get refs "docs")))))))))

(deftest batch-creation-rolls-back-on-invalid-edge-target
  (with-db
    (fn [ds]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"symbolic targets only resolve to batch refs"
                            (db/add-task-batch!
                             ds
                             [{:ref 'docs
                               :title "Docs"
                               :edges [{:type "depends-on" :to 'missing}]}])))
      (is (empty? (db/all-tasks ds))))))

(deftest batch-creation-validates-input-shape
  (with-db
    (fn [ds]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Duplicate batch ref"
                            (db/add-task-batch! ds [{:ref 'x :title "A"}
                                                    {:ref 'x :title "B"}])))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Unknown keys"
                            (db/add-task-batch! ds [{:title "A" :edge []}])))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Attributes must be nil or an EDN map"
                            (db/add-task-batch! ds [{:title "A" :attributes {:status 'todo}}]))))))
