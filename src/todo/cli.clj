(ns todo.cli
  (:gen-class)
  (:require [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.tools.cli :refer [parse-opts]]
            [todo.client :as client]
            [todo.daemon.runtime :as runtime]
            [todo.db :as db]
            [todo.query :as query]
            [todo.specs :as specs]))

(def query-commands #{"show" "list" "ready"})
(def commands (conj query-commands "init" "add" "update" "daemon"))

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

(def query-options
  [[nil "--where EDN" "EDN query expression"
    :id :where
    :parse-fn edn/read-string]
   [nil "--query NAME" "Named query from --query-file"
    :id :query
    :parse-fn symbol]
   [nil "--query-file PATH" "EDN map of query names to query definitions"
    :id :query-file]
   [nil "--param KEY=VALUE" "Repeatable string query parameter"
    :id :param
    :multi true
    :default {}
    :parse-fn (fn [s]
                (let [[k v] (str/split s #"=" 2)]
                  (when (or (str/blank? k) (nil? v))
                    (throw (ex-info (str "Malformed query parameter: " s) {:param s})))
                  [(keyword k) v]))
    :update-fn (fn [params [k v]] (assoc params k v))]])

(def daemon-start-options
  [[nil "--config PATH" "Daemon startup EDN config; supports {:load-files [\"trusted.clj\"]}"
    :id :config]])

(defn usage [summary]
  (str "Todo CLI\n\n"
       "Usage:\n"
       "  clojure -M:todo [--db <path>] [--format human|edn|json] <command> [args]\n\n"
       "Commands:\n"
       "  init\n"
       "  add <title> [--status status] [--attr key=value ...]\n"
       "  update <id> [--title title] [--status status] [--attr key=value ...] [--edge edge-type:to-id ...]\n"
       "  show <id>\n"
       "  list [--where EDN | --query name --query-file path] [--param key=value ...]\n"
       "  ready [--where EDN | --query name --query-file path] [--param key=value ...]\n"
       "  daemon start [--config path]\n"
       "  daemon stop\n"
       "  daemon status\n\n"
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

(defn parse-options [args option-spec summary]
  (let [{:keys [options arguments errors]} (parse-opts args option-spec)]
    (when (seq errors) (fail! (str/join "\n" errors) summary))
    (when (seq arguments) (fail! (str "Unknown or misplaced argument: " (first arguments)) summary))
    (when (contains? options :attr)
      (when-not (s/valid? ::specs/cli-attributes (:attr options))
        (fail! (str "Invalid attributes:\n" (explain ::specs/cli-attributes (:attr options))) summary)))
    options))

(defn parse-command-options [args summary]
  (parse-options args command-options summary))

(defn parse-query-options [args summary]
  (let [{:keys [options arguments errors]} (parse-opts args query-options)]
    (when (seq errors) (fail! (str/join "\n" errors) summary))
    (when (seq arguments) (fail! (str "Unknown or misplaced argument: " (first arguments)) summary))
    (when (and (:where options) (:query options))
      (fail! "Use either --where or --query, not both" summary))
    (when (and (:query options) (not (:query-file options)))
      (fail! "--query requires --query-file" summary))
    (when (and (seq (:param options)) (not (or (:where options) (:query options))))
      (fail! "--param requires --where or --query" summary))
    options))

(defn query-from-options [options]
  (cond
    (:where options) (:where options)
    (:query options) (query/query-def (query/read-edn-file (:query-file options)) (:query options))
    :else nil))

(defn parse-daemon-start-options [args summary]
  (parse-options args daemon-start-options summary))

(defn print-result [format result]
  (case format
    "human" (if (and (sequential? result) (empty? result))
              (println "(no rows)")
              (doseq [row (if (sequential? result) result [result])] (prn row)))
    "edn" (prn result)
    "json" (println (json/write-str result))))

(defn run-command! [db-file command args summary]
  (case command
    "init" (do (require-conform ::specs/empty-command args command summary)
                (client/init db-file))
    "add" (let [{:keys [title opts]} (require-conform ::specs/add-command args command summary)
                 options (parse-command-options opts summary)]
             (when (seq (:edge options))
               (fail! "add does not accept --edge" summary))
             (client/add db-file {:title title :status (:status options) :attributes (:attr options)}))
    "update" (let [{:keys [id opts]} (require-conform ::specs/update-command args command summary)
                    options (parse-command-options opts summary)]
                (client/update db-file id {:title (:title options)
                                           :status (:status options)
                                           :attributes (when (seq (:attr options)) (:attr options))
                                           :edges (:edge options)}))
    "show" (do (require-conform ::specs/one-id-command args command summary) (client/show db-file (first args)))
    "list" (let [options (parse-query-options args summary)
                 query-def (query-from-options options)]
             (if query-def
               (client/list db-file query-def (:param options))
               (client/list db-file)))
    "ready" (let [options (parse-query-options args summary)
                  query-def (query-from-options options)]
              (if query-def
                (client/ready db-file query-def (:param options))
                (client/ready db-file)))))

(defn daemon-status [db-file]
  (let [meta (client/status db-file)]
    {:health "ok"
     :canonical-db-path (:canonical-db-path meta)
     :pid (:pid meta)
     :endpoint (:endpoint meta)
     :identity {:nonce (:nonce meta)}}))

(defn run-daemon-command! [db-file args summary]
  (let [subcommand (first args)
        subargs (vec (rest args))]
    (case subcommand
      "start" (let [options (parse-daemon-start-options subargs summary)]
                 (runtime/start! db-file {:config-file (:config options)})
                 (println "daemon started")
                 (while @runtime/current-runtime
                   (Thread/sleep 100)))
      "stop" (do (require-conform ::specs/empty-command subargs "daemon stop" summary)
                  (client/stop db-file))
      "status" (do (require-conform ::specs/empty-command subargs "daemon status" summary)
                    (daemon-status db-file))
      (fail! (str "Unknown daemon command: " (or subcommand "")) summary))))

(defn -main [& args]
  (let [[opts command command-args summary] (parse-global-options args)]
    (when (nil? command) (fail! "Missing command" summary))
    (when-not (commands command) (fail! (str "Unknown command: " command) summary))
    (try
      (let [result (if (= command "daemon")
                     (run-daemon-command! (:db opts) command-args summary)
                     (run-command! (:db opts) command command-args summary))]
        (cond
          (and (= command "add") (= "human" (:format opts))) (println (:id result))
          (and (= command "daemon") (= "start" (first command-args))) nil
          (or (query-commands command) (= command "daemon") (not= "human" (:format opts))) (print-result (:format opts) result)))
      (catch clojure.lang.ExceptionInfo e (fail! (.getMessage e) summary))
      (catch Exception e (fail! (.getMessage e) summary)))))
