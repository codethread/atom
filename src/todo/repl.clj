(ns todo.repl
  (:require [todo.client :as client]
            [todo.query :as query]))

(defonce ^:private active-db-file (atom nil))
(defonce ^:private query-registry (atom {}))

(defn- db-file []
  (or @active-db-file
      (throw (ex-info "No todo daemon is open. Start a daemon for the database, then call (open! \"path/to/todo.sqlite\") before using todo.repl helpers."
                      {:helper 'open!}))))

(defn open! [db-file]
  (reset! active-db-file nil)
  (client/status db-file)
  (reset! active-db-file db-file))

(defn init! []
  (client/init (db-file)))

(defn task!
  ([title]
   (task! title {}))
  ([title attributes]
   (client/add (db-file) {:title title :attributes attributes}))
  ([title status attributes]
   (client/add (db-file) {:title title :status status :attributes attributes})))

(defn update!
  ([id patch]
   (client/update (db-file) id patch)))

(defn task [id]
  (client/show (db-file) id))

(defn defquery! [query-name query-def]
  (query/validate-query-def! query-def)
  (swap! query-registry assoc query-name query-def)
  query-name)

(defn load-queries! [path]
  (let [registry (query/read-edn-file path)]
    (when-not (map? registry)
      (throw (ex-info "Query file must contain one EDN map of query names to query definitions" {:path path})))
    (doseq [[query-name query-def] registry]
      (when-not (or (symbol? query-name) (keyword? query-name))
        (throw (ex-info "Query names must be symbols or keywords" {:query query-name})))
      (query/validate-query-def! query-def))
    (swap! query-registry merge registry)
    (keys registry)))

(defn queries []
  @query-registry)

(defn- resolve-query [query-or-def]
  (if (or (symbol? query-or-def) (keyword? query-or-def))
    (query/query-def @query-registry query-or-def)
    query-or-def))

(defn query
  ([query-or-def]
   (query query-or-def {}))
  ([query-or-def params]
   (client/list (db-file) (resolve-query query-or-def) params)))

(defn tasks
  ([]
   (client/list (db-file)))
  ([query-or-def]
   (query query-or-def))
  ([query-or-def params]
   (query query-or-def params)))

(defn ready
  ([]
   (client/ready (db-file)))
  ([query-or-def]
   (ready query-or-def {}))
  ([query-or-def params]
   (client/ready (db-file) (resolve-query query-or-def) params)))
