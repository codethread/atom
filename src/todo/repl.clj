(ns todo.repl
  (:require [todo.client :as client]
            [todo.query :as query]))

(defonce ^:private active-db-file (atom nil))

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

(defn- call-daemon [f]
  (try
    (f)
    (catch clojure.lang.ExceptionInfo e
      (if-let [daemon-message (:daemon-message (ex-data e))]
        (throw (ex-info daemon-message (:daemon-data (ex-data e))))
        (throw e)))))

(defn defquery! [query-name query-def]
  (let [db (db-file)]
    (call-daemon #(client/register-query db query-name query-def))))

(defn load-queries! [path]
  (let [db (db-file)
        registry (query/read-edn-file path)]
    (when-not (map? registry)
      (throw (ex-info "Query file must contain one EDN map of query names to query definitions" {:path path})))
    (call-daemon #(client/load-queries db registry))))

(defn queries []
  (let [db (db-file)]
    (call-daemon #(client/queries db))))

(defn- named-query? [query-or-def]
  (or (symbol? query-or-def) (keyword? query-or-def)))

(defn- run-query [db query-or-def params ad-hoc named]
  (call-daemon #(if (named-query? query-or-def)
                  (named db query-or-def params)
                  (ad-hoc db query-or-def params))))

(defn query
  ([query-or-def]
   (query query-or-def {}))
  ([query-or-def params]
   (run-query (db-file) query-or-def params client/list client/list-query)))

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
   (run-query (db-file) query-or-def params client/ready client/ready-query)))
