(ns todo.cli
  (:gen-class)
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.tools.cli :refer [parse-opts]]
            [todo.db :as db]
            [todo.specs :as specs]))

(def query-commands #{"show" "list" "deps" "transitive-deps" "blocking" "ready" "by-attr"})
(def commands (conj query-commands "init" "add" "link" "done"))

(def global-options
  [[nil "--db PATH" "SQLite database path"
    :id :db
    :default db/default-db-file]
   [nil "--format FORMAT" "Output mode for query commands: human, edn, json"
    :id :format
    :default "human"
    :validate [#(s/valid? ::specs/format %) "must be one of: human, edn, json"]]])

(def attr-options
  [[nil "--attr ATTR" "Repeatable string task or edge attribute: key=value"
    :id :attr
    :multi true
    :default {}
    :parse-fn (fn [s]
                (let [[k v] (str/split s #"=" 2)]
                  (when (or (str/blank? k) (nil? v))
                    (throw (ex-info (str "Malformed attribute: " s) {:attr s})))
                  [(keyword k) v]))
    :update-fn (fn [attrs [k v]] (assoc attrs k v))]])

(defn usage [summary]
  (str "Todo agent CLI\n"
       "\n"
       "Usage:\n"
       "  clojure -M:todo [--db <path>] [--format human|edn|json] <command> [args]\n"
       "\n"
       "Commands:\n"
       "  init\n"
       "  add <id> <title> [--attr key=value ...]\n"
       "  link <from-id> <to-id> <edge-type> [--attr key=value ...]\n"
       "  show <id>\n"
       "  list\n"
       "  deps <id>\n"
       "  transitive-deps <id>\n"
       "  blocking <id>\n"
       "  ready\n"
       "  by-attr <key> <value>\n"
       "  done <id>\n"
       "\n"
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
  (-> (s/explain-str spec value)
      (str/replace #"\n$" "")))

(defn require-conform [spec args command summary]
  (let [conformed (s/conform spec args)]
    (when (= ::s/invalid conformed)
      (fail! (str "Invalid arguments for " command ":\n" (explain spec args)) summary))
    conformed))

(defn parse-global-options [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args global-options :in-order true)]
    (when (seq errors)
      (fail! (str/join "\n" errors) summary))
    (when-not (s/valid? ::specs/opts options)
      (fail! (str "Invalid options:\n" (explain ::specs/opts options)) summary))
    [options (first arguments) (vec (rest arguments)) summary]))

(defn parse-attrs [args summary]
  (let [{:keys [options arguments errors]} (parse-opts args attr-options)]
    (when (seq errors)
      (fail! (str/join "\n" errors) summary))
    (when (seq arguments)
      (fail! (str "Unknown or misplaced argument: " (first arguments)) summary))
    (when-not (s/valid? ::specs/cli-attributes (:attr options))
      (fail! (str "Invalid attributes:\n" (explain ::specs/cli-attributes (:attr options))) summary))
    (:attr options)))

(def json-columns #{:attributes :edge_attributes})

(defn normalize-row [row]
  (reduce-kv (fn [m k v]
               (assoc m k (if (and (json-columns k) (string? v))
                             (db/<-json v)
                             v)))
             {}
             row))

(defn normalize [result]
  (cond
    (map? result) (normalize-row result)
    (sequential? result) (mapv normalize-row result)
    :else result))

(defn print-result [format result]
  (let [result (normalize result)]
    (case format
      "human" (if (and (sequential? result) (empty? result))
                (println "(no rows)")
                (doseq [row (if (sequential? result) result [result])]
                  (prn row)))
      "edn" (prn result)
      "json" (println (json/write-str result)))))

(defn run-command! [ds command args summary]
  (case command
    "init" (do
             (require-conform ::specs/empty-command args command summary)
             (db/init! ds)
             {:database "initialized"})
    "add" (let [{:keys [id title attrs]} (require-conform ::specs/add-command args command summary)]
            (db/add-task! ds {:id id :title title :attributes (parse-attrs attrs summary)}))
    "link" (let [{:keys [from to type attrs]} (require-conform ::specs/link-command args command summary)]
             (db/add-edge! ds {:from from :to to :type type :attributes (parse-attrs attrs summary)}))
    "show" (do (require-conform ::specs/one-id-command args command summary) (db/get-task ds (first args)))
    "list" (do (require-conform ::specs/empty-command args command summary) (db/all-tasks ds))
    "deps" (do (require-conform ::specs/one-id-command args command summary) (db/task-dependencies ds (first args)))
    "transitive-deps" (do (require-conform ::specs/one-id-command args command summary) (db/transitive-dependencies ds (first args)))
    "blocking" (do (require-conform ::specs/one-id-command args command summary) (db/blocking-tasks ds (first args)))
    "ready" (do (require-conform ::specs/empty-command args command summary) (db/ready-tasks ds))
    "by-attr" (let [{:keys [key value]} (require-conform ::specs/by-attr-command args command summary)]
                (db/tasks-by-attribute ds (keyword key) value))
    "done" (do (require-conform ::specs/one-id-command args command summary) (db/update-task-status! ds (first args) "done"))))

(defn -main [& args]
  (let [[opts command command-args summary] (parse-global-options args)]
    (when (nil? command)
      (fail! "Missing command" summary))
    (when-not (commands command)
      (fail! (str "Unknown command: " command) summary))
    (try
      (let [result (run-command! (db/datasource (:db opts)) command command-args summary)]
        (when (or (query-commands command) (not= "human" (:format opts)))
          (print-result (:format opts) result)))
      (catch clojure.lang.ExceptionInfo e
        (fail! (.getMessage e) summary))
      (catch Exception e
        (fail! (.getMessage e) summary)))))
