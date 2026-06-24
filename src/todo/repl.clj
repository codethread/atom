(ns todo.repl
  (:require [next.jdbc :as jdbc]
            [todo.db :as db]))

(defonce ^:private active-datasource (atom nil))

(defn- ds []
  (or @active-datasource
      (throw (ex-info "No todo database is open. Call (open! \"path/to/todo.sqlite\") before using todo.repl helpers."
                      {:helper 'open!}))))

(def ^:private json-columns #{:attributes :edge_attributes :blockers})

(defn- unpack-json [row]
  (reduce-kv (fn [m k v]
               (assoc m k (if (and (json-columns k) (string? v))
                            (db/<-json v)
                            v)))
             {}
             row))

(defn- unpack [x]
  (cond
    (map? x) (unpack-json x)
    (sequential? x) (mapv unpack-json x)
    :else x))

(defn open! [db-file]
  (reset! active-datasource (db/datasource db-file)))

(defn init! []
  (db/init! (ds)))

(defn task!
  ([title]
   (task! title {}))
  ([title attributes]
   (unpack (db/add-task! (ds) {:title title :attributes attributes})))
  ([title status attributes]
   (unpack (db/add-task! (ds) {:title title :status status :attributes attributes}))))

(defn update!
  ([id patch]
   (let [{:keys [title status attributes edges]} patch]
     (jdbc/with-transaction [tx (ds)]
       (doseq [{:keys [to type attributes]} edges]
         (db/add-edge! tx {:from id :to to :type type :attributes attributes}))
       (unpack (db/update-task! tx id {:title title :status status :attributes attributes}))))))

(defn task [id]
  (unpack (db/get-task (ds) id)))

(defn tasks []
  (unpack (db/all-tasks (ds))))

(defn ready []
  (unpack (db/ready-tasks (ds))))
