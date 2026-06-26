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

(deftest init-creates-missing-database-parent-directories
  (let [root (java.nio.file.Files/createTempDirectory "todo-db-parent" (make-array java.nio.file.attribute.FileAttribute 0))
        db-file (.getAbsolutePath (java.io.File. (.toFile root) "nested/path/todo.sqlite"))]
    (try
      (db/init! (db/datasource db-file))
      (is (.isFile (java.io.File. db-file)))
      (finally
        (delete-sqlite-family! db-file)
        (doseq [f (reverse (file-seq (.toFile root)))]
          (.delete f))))))

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

(deftest task-query-dsl-filters-fields-attributes-and-params
  (with-db
    (fn [ds]
      (let [agent (:id (db/add-task! ds {:title "Agent docs" :attributes {:owner "agent" :priority "high"}}))
            human (:id (db/add-task! ds {:title "Human docs" :attributes {:owner "human" :priority "low"}}))
            dotted (:id (db/add-task! ds {:title "Dotted" :attributes {"team.owner" "agent"}}))]
        (testing "ad hoc queries filter task fields and attributes"
          (is (= [agent]
                 (mapv :id (db/all-tasks ds [:and
                                              [:= :status "todo"]
                                              [:= [:attr :owner] "agent"]])))))
        (testing "named function queries accept runtime parameters"
          (is (= [human]
                 (mapv :id (db/all-tasks ds {:params [:owner]
                                              :where [:= [:attr :owner] [:param :owner]]}
                                         {:owner "human"})))))
        (testing "attribute paths can query legal keys that need JSON path quoting"
          (is (= [dotted]
                 (mapv :id (db/all-tasks ds [:= [:attr "team.owner"] "agent"])))))))))

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

(deftest set-oriented-query-and-hydration-primitives
  (with-db
    (fn [ds]
      (let [agent (:id (db/add-task! ds {:title "Agent" :attributes {:owner "agent"}}))
            human (:id (db/add-task! ds {:title "Human" :attributes {:owner "human"}}))
            other (:id (db/add-task! ds {:title "Other" :attributes {:owner "agent"}}))]
        (testing "query-task-ids shares query compilation and stable id ordering"
          (is (= (sort [agent other])
                 (db/query-task-ids ds [:= [:attr :owner] "agent"])))
          (is (= [human]
                 (db/query-task-ids ds {:params [:owner]
                                        :where [:= [:attr :owner] [:param :owner]]}
                                    {:owner "human"}))))
        (testing "tasks-by-ids preserves first occurrence order and collapses duplicates"
          (is (= [other agent human]
                 (mapv :id (db/tasks-by-ids ds [other agent other human agent])))))
        (testing "tasks-by-ids handles empty input"
          (is (= [] (db/tasks-by-ids ds []))))
        (testing "tasks-by-ids fails loudly for missing ids"
          (is (thrown-with-msg? clojure.lang.ExceptionInfo
                                #"Task ids not found"
                                (db/tasks-by-ids ds [agent "missing-task"]))))))))

(deftest parent-of-ancestor-root-primitives
  (with-db
    (fn [ds]
      (let [feature-a (:id (db/add-task! ds {:title "Feature A" :attributes {:kind "feature"}}))
            feature-b (:id (db/add-task! ds {:title "Feature B" :attributes {:kind "epic"}}))
            epic (:id (db/add-task! ds {:title "Epic" :attributes {:kind "epic"}}))
            branch (:id (db/add-task! ds {:title "Branch" :attributes {:kind "feature"}}))
            leaf (:id (db/add-task! ds {:title "Leaf" :attributes {:kind "task"}}))]
        (db/add-edge! ds {:from epic :to feature-a :type "parent-of" :attributes {}})
        (db/add-edge! ds {:from feature-a :to branch :type "parent-of" :attributes {}})
        (db/add-edge! ds {:from feature-b :to branch :type "parent-of" :attributes {}})
        (db/add-edge! ds {:from branch :to leaf :type "parent-of" :attributes {}})

        (testing "without :where ancestor-root-ids returns graph roots across multi-parent paths"
          (is (= (sort [epic feature-b])
                 (db/ancestor-root-ids ds [leaf leaf]))))
        (testing "with :where it returns topmost matching ancestors per path"
          (is (= (sort [branch feature-a])
                 (db/ancestor-root-ids ds [leaf]
                                       {:where [:= [:attr :kind] "feature"]}))))
        (testing "seed ids are depth-zero candidates"
          (is (= [feature-a]
                 (db/ancestor-root-ids ds [feature-a]
                                       {:where [:= [:attr :kind] "feature"]}))))
        (testing "ancestor-root-ids handles empty input and missing seeds"
          (is (= [] (db/ancestor-root-ids ds [])))
          (is (thrown-with-msg? clojure.lang.ExceptionInfo
                                #"Task ids not found"
                                (db/ancestor-root-ids ds ["missing-seed"]))))))))

(deftest parent-of-subgraph-primitive
  (with-db
    (fn [ds]
      (let [root (:id (db/add-task! ds {:title "Root" :attributes {}}))
            child-a (:id (db/add-task! ds {:title "Child A" :attributes {}}))
            child-b (:id (db/add-task! ds {:title "Child B" :attributes {}}))
            grandchild (:id (db/add-task! ds {:title "Grandchild" :attributes {}}))
            outside (:id (db/add-task! ds {:title "Outside" :attributes {}}))]
        (db/add-edge! ds {:from root :to child-a :type "parent-of" :attributes {:order "a"}})
        (db/add-edge! ds {:from root :to child-b :type "parent-of" :attributes {:order "b"}})
        (db/add-edge! ds {:from child-a :to grandchild :type "parent-of" :attributes {}})
        (db/add-edge! ds {:from outside :to root :type "parent-of" :attributes {}})

        (testing "subgraph includes roots, descendants, and only internal parent-of edges"
          (let [result (db/subgraph ds [root root child-b])]
            (is (= [root child-b] (:root-ids result)))
            (is (= (sort [root child-a child-b grandchild])
                   (mapv :id (:tasks result))))
            (is (= (sort-by identity [[root child-a "parent-of"]
                                        [root child-b "parent-of"]
                                        [child-a grandchild "parent-of"]])
                   (mapv (juxt :from_task_id :to_task_id :edge_type) (:edges result))))))
        (testing "subgraph handles empty roots and missing roots"
          (is (= {:root-ids [] :tasks [] :edges []} (db/subgraph ds [])))
          (is (thrown-with-msg? clojure.lang.ExceptionInfo
                                #"Task ids not found"
                                (db/subgraph ds ["missing-root"]))))))))

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
