(ns todo.cli
  (:gen-class)
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.tools.cli :refer [parse-opts]]
            [next.jdbc :as jdbc]
            [todo.db :as db]
            [todo.specs :as specs]))

(def query-commands #{"show" "list" "ready"})
(def commands (conj query-commands "init" "add" "update"))

(def global-options
  [[nil "--db PATH" "SQLite database path"
    :id :db
    :default db/default-db-file]
   [nil "--format FORMAT" "Output mode: human, edn, json"
    :id :format
    :default "human"
    :validate [#(s/valid? ::specs/format %) "must be one of: human, edn, json"]]])

(def command-options
  [[nil "--title TITLE" "Replacement task title"
    :id :title]
   [nil "--status STATUS" "Task status: todo, done, failed, cancelled"
    :id :status
    :validate [#(contains? specs/allowed-statuses %) "must be one of: todo, done, failed, cancelled"]]
   [nil "--attr ATTR" "Repeatable string task attribute patch: key=value"
    :id :attr
    :multi true
    :default {}
    :parse-fn (fn [s]
                (let [[k v] (str/split s #"=" 2)]
                  (when (or (str/blank? k) (nil? v))
                    (throw (ex-info (str "Malformed attribute: " s) {:attr s})))
                  [(keyword k) v]))
    :update-fn (fn [attrs [k v]] (assoc attrs k v))]
   [nil "--edge EDGE" "Repeatable task edge: edge-type:to-id"
    :id :edge
    :multi true
    :default []
    :parse-fn (fn [s]
                (let [separator (.lastIndexOf s ":")
                      type (when (not= -1 separator) (subs s 0 separator))
                      to (when (not= -1 separator) (subs s (inc separator)))]
                  (when (or (str/blank? type) (str/blank? to))
                    (throw (ex-info (str "Malformed edge: " s) {:edge s})))
                  {:type type :to to}))
    :update-fn conj]])

(defn usage [summary]
  (str "Todo CLI\n\n"
       "Usage:\n"
       "  clojure -M:todo [--db <path>] [--format human|edn|json] <command> [args]\n\n"
       "Commands:\n"
       "  init\n"
       "  add <title> [--status status] [--attr key=value ...]\n"
       "  update <id> [--title title] [--status status] [--attr key=value ...] [--edge edge-type:to-id ...]\n"
       "  show <id>\n"
       "  list\n"
       "  ready\n\n"
       "Options:\n"
       summary
       "\n"))

(defn fail! [message summary]
  (binding [*out* *err*]
    (println "Error:" message)
    (println)
    (println (usage summary)))
  (System/exit 1))

(defn explain [spec value]
  (-> (s/explain-str spec value) (str/replace #"\n$" "")))

(defn require-conform [spec args command summary]
  (let [conformed (s/conform spec args)]
    (when (= ::s/invalid conformed)
      (fail! (str "Invalid arguments for " command ":\n" (explain spec args)) summary))
    conformed))

(defn parse-global-options [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args global-options :in-order true)]
    (when (seq errors) (fail! (str/join "\n" errors) summary))
    (when-not (s/valid? ::specs/opts options)
      (fail! (str "Invalid options:\n" (explain ::specs/opts options)) summary))
    [options (first arguments) (vec (rest arguments)) summary]))

(defn parse-command-options [args summary]
  (let [{:keys [options arguments errors]} (parse-opts args command-options)]
    (when (seq errors) (fail! (str/join "\n" errors) summary))
    (when (seq arguments) (fail! (str "Unknown or misplaced argument: " (first arguments)) summary))
    (when-not (s/valid? ::specs/cli-attributes (:attr options))
      (fail! (str "Invalid attributes:\n" (explain ::specs/cli-attributes (:attr options))) summary))
    options))

(def json-columns #{:attributes :edge_attributes})

(declare normalize)

(defn normalize-row [row]
  (reduce-kv (fn [m k v]
               (assoc m k (cond
                            (and (json-columns k) (string? v)) (db/<-json v)
                            (map? v) (normalize v)
                            (sequential? v) (mapv normalize v)
                            :else v)))
             {}
             row))

(defn normalize [result]
  (cond
    (map? result) (normalize-row result)
    (sequential? result) (mapv normalize result)
    :else result))

(defn print-result [format result]
  (let [result (normalize result)]
    (case format
      "human" (if (and (sequential? result) (empty? result))
                (println "(no rows)")
                (doseq [row (if (sequential? result) result [result])] (prn row)))
      "edn" (prn result)
      "json" (println (json/write-str result)))))

(defn apply-edges! [ds id edges]
  (doseq [{:keys [to type]} edges]
    (when-not (db/get-task ds to)
      (throw (ex-info "Edge target task not found" {:to to :type type})))
    (db/add-edge! ds {:from id :to to :type type :attributes {}})))

(defn run-command! [ds command args summary]
  (case command
    "init" (do (require-conform ::specs/empty-command args command summary)
                (db/init! ds)
                {:database "initialized"})
    "add" (let [{:keys [title opts]} (require-conform ::specs/add-command args command summary)
                 options (parse-command-options opts summary)
                 created (db/add-task! ds {:title title :status (:status options) :attributes (:attr options)})]
             created)
    "update" (let [{:keys [id opts]} (require-conform ::specs/update-command args command summary)
                    options (parse-command-options opts summary)]
                (jdbc/with-transaction [tx ds]
                  (apply-edges! tx id (:edge options))
                  (db/update-task! tx id {:title (:title options)
                                          :status (:status options)
                                          :attributes (when (seq (:attr options)) (:attr options))})))
    "show" (do (require-conform ::specs/one-id-command args command summary) (db/get-task ds (first args)))
    "list" (do (require-conform ::specs/empty-command args command summary) (db/all-tasks ds))
    "ready" (do (require-conform ::specs/empty-command args command summary) (db/ready-tasks ds))))

(defn -main [& args]
  (let [[opts command command-args summary] (parse-global-options args)]
    (when (nil? command) (fail! "Missing command" summary))
    (when-not (commands command) (fail! (str "Unknown command: " command) summary))
    (try
      (let [result (run-command! (db/datasource (:db opts)) command command-args summary)]
        (cond
          (and (= command "add") (= "human" (:format opts))) (println (:id result))
          (or (query-commands command) (not= "human" (:format opts))) (print-result (:format opts) result)))
      (catch clojure.lang.ExceptionInfo e (fail! (.getMessage e) summary))
      (catch Exception e (fail! (.getMessage e) summary)))))
