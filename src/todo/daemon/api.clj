(ns todo.daemon.api
  (:refer-clojure :exclude [list update])
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.repl.deps :as repl-deps]
            [clojure.string :as str]
            [next.jdbc :as jdbc]
            [todo.daemon.runtime :as runtime]
            [todo.db :as db]
            [todo.query :as query])
  (:import [java.time Instant]))

(defn normalize-row [row]
  (cond-> row
    (string? (:attributes row)) (clojure.core/update :attributes db/<-json)))

(defn normalize [result]
  (cond
    (map? result) (normalize-row result)
    (sequential? result) (mapv normalize-row result)
    :else result))

(defn- ds [runtime]
  (:datasource runtime))

(defn- query-registry [runtime]
  (:query-registry runtime))

(defn- plugin-registry [runtime]
  (:plugin-registry runtime))

(defn- approved-lib-sync-state [runtime]
  (:approved-lib-sync-state runtime))

(defn- with-library-classloader [runtime f]
  (let [thread (Thread/currentThread)
        previous-loader (.getContextClassLoader thread)
        loader (:library-classloader runtime)]
    (try
      (.setContextClassLoader thread loader)
      (f)
      (finally
        (.setContextClassLoader thread previous-loader)))))

(defn- config-dir [runtime]
  (get-in runtime [:metadata :config-dir]))

(defn- libs-file [runtime]
  (io/file (config-dir runtime) "libs.edn"))

(defn- canonical-root [runtime path]
  (let [file (io/file path)
        resolved (if (.isAbsolute file)
                   file
                   (io/file (config-dir runtime) path))]
    (.getCanonicalPath resolved)))

(defn- validate-approved-lib-entry! [lib entry]
  (when-not (symbol? lib)
    (throw (ex-info "Library coordinate must be a symbol" {:lib lib})))
  (when-not (map? entry)
    (throw (ex-info "Library entry must be a map" {:lib lib :entry entry})))
  (when-let [unknown (seq (remove #{:local/root} (keys entry)))]
    (throw (ex-info "Library entry contains unknown keys" {:lib lib :keys (vec unknown)})))
  (when-not (and (string? (:local/root entry)) (not (str/blank? (:local/root entry))))
    (throw (ex-info "Library entry requires non-blank string :local/root" {:lib lib :local/root (:local/root entry)}))))

(defn normalize-approved-libs [runtime config]
  (when-not (map? config)
    (throw (ex-info "libs.edn must contain a map" {:config config})))
  (when-let [unknown (seq (remove #{:libs} (keys config)))]
    (throw (ex-info "libs.edn contains unknown top-level keys" {:keys (vec unknown)})))
  (when-not (map? (:libs config))
    (throw (ex-info "libs.edn requires :libs map" {:libs (:libs config)})))
  {:libs (into {}
               (map (fn [[lib entry]]
                      (validate-approved-lib-entry! lib entry)
                      [lib {:local/root (:local/root entry)
                            :root (canonical-root runtime (:local/root entry))}]))
               (:libs config))})

(defn approved-libs [runtime]
  (let [file (libs-file runtime)]
    (cond
      (not (.exists file))
      {:libs {}}

      (not (.isFile file))
      (throw (ex-info "libs.edn is malformed or unreadable" {:file (.getPath file)}))

      :else
      (normalize-approved-libs
       runtime
       (try
         (edn/read-string (slurp file))
         (catch Throwable t
           (throw (ex-info "libs.edn is malformed or unreadable" {:file (.getPath file)} t))))))))

(defn- sync-failed [lib entry reason data]
  [lib (merge {:lib lib
               :local/root (:local/root entry)
               :root (:root entry)
               :status :failed
               :reason reason}
              data)])

(defn- sync-approved-lib! [runtime lib entry]
  (let [root-file (io/file (:root entry))]
    (cond
      (not (.exists root-file))
      (sync-failed lib entry :missing-root {})

      (not (.isDirectory root-file))
      (sync-failed lib entry :unreadable-root {})

      (not (.canRead root-file))
      (sync-failed lib entry :unreadable-root {})

      :else
      (try
        (let [added (with-library-classloader
                      runtime
                      #(binding [clojure.core/*repl* true]
                         (repl-deps/add-libs {lib {:local/root (:root entry)}})))]
          [lib {:lib lib
                :local/root (:local/root entry)
                :root (:root entry)
                :status (if (seq added) :loaded :already-available)}])
        (catch Throwable t
          (sync-failed lib entry :runtime-add-failed {:message (ex-message t)
                                                      :class (str (class t))}))))))

(defn sync-approved-libs [runtime]
  (reset! (approved-lib-sync-state runtime) {})
  (let [approved (approved-libs runtime)
        results (into (sorted-map)
                      (map (fn [[lib entry]] (sync-approved-lib! runtime lib entry)))
                      (:libs approved))]
    (reset! (approved-lib-sync-state runtime) results)
    {:libs results}))

(defn approved-lib-syncs [runtime]
  {:libs (into (sorted-map) @(approved-lib-sync-state runtime))})

(def supported-plugin-format-version 1)
(def plugin-authored-keys #{:format-version :name :version :requires-atom :provides})

(defn canonical-plugin-name [plugin-name]
  (cond
    (symbol? plugin-name) plugin-name
    (keyword? plugin-name) (symbol (namespace plugin-name) (name plugin-name))
    :else (throw (ex-info "Plugin name must be a symbol or keyword" {:name plugin-name}))))

(defn- canonical-provides [provides]
  (when-not (vector? provides)
    (throw (ex-info "Plugin :provides must be a vector" {:provides provides})))
  (mapv canonical-plugin-name provides))

(defn- validate-plugin-metadata-keys! [metadata allowed-keys]
  (when-not (map? metadata)
    (throw (ex-info "Plugin metadata must be a map" {:metadata metadata})))
  (let [keys-present (set (keys metadata))
        unknown (seq (remove allowed-keys keys-present))]
    (when unknown
      (throw (ex-info "Plugin metadata contains unknown keys" {:keys (vec unknown)})))))

(defn- normalize-plugin-metadata! [metadata]
  (when-not (contains? metadata :format-version)
    (throw (ex-info "Plugin metadata requires :format-version" {})))
  (when-not (= supported-plugin-format-version (:format-version metadata))
    (throw (ex-info "Unsupported plugin metadata format version" {:format-version (:format-version metadata)})))
  (when-not (contains? metadata :name)
    (throw (ex-info "Plugin metadata requires :name" {})))
  (when (and (contains? metadata :version) (not (string? (:version metadata))))
    (throw (ex-info "Plugin :version must be a string" {:version (:version metadata)})))
  (when (and (contains? metadata :requires-atom) (not (string? (:requires-atom metadata))))
    (throw (ex-info "Plugin :requires-atom must be a string" {:requires-atom (:requires-atom metadata)})))
  (cond-> (assoc metadata :name (canonical-plugin-name (:name metadata)))
    (contains? metadata :provides) (clojure.core/update :provides canonical-provides)))

(defn validate-plugin-metadata! [metadata]
  (validate-plugin-metadata-keys! metadata plugin-authored-keys)
  (normalize-plugin-metadata! metadata))

(defn register-plugin [runtime metadata]
  (let [recorded (validate-plugin-metadata! metadata)]
    (swap! (plugin-registry runtime) assoc (:name recorded) recorded)
    recorded))

(defn plugins [runtime]
  (vec (vals (into (sorted-map) @(plugin-registry runtime)))))

(defn plugin [runtime plugin-name]
  (get @(plugin-registry runtime) (canonical-plugin-name plugin-name)))

(defn- canonical-plugin-dir [runtime path]
  (let [file (io/file path)
        resolved (if (.isAbsolute file)
                   file
                   (io/file (get-in runtime [:metadata :config-dir]) path))]
    (.getCanonicalFile resolved)))

(defn load-plugin [runtime path]
  (let [dir (canonical-plugin-dir runtime path)
        metadata-file (io/file dir "atom-plugin.edn")
        init-file (io/file dir "init.clj")]
    (when-not (.isDirectory dir)
      (throw (ex-info "Plugin directory does not exist" {:path path :dir (.getPath dir)})))
    (when-not (.isFile metadata-file)
      (throw (ex-info "Plugin metadata file is missing" {:dir (.getPath dir)
                                                          :file (.getPath metadata-file)})))
    (when-not (.isFile init-file)
      (throw (ex-info "Plugin init.clj is missing" {:dir (.getPath dir)
                                                     :file (.getPath init-file)})))
    (let [metadata (validate-plugin-metadata!
                    (try
                      (edn/read-string (slurp metadata-file))
                      (catch Throwable t
                        (throw (ex-info "Plugin metadata is malformed" {:file (.getPath metadata-file)} t)))))
          init-path (.getCanonicalPath init-file)]
      (try
        (load-file init-path)
        (catch Throwable t
          (throw (ex-info "Plugin init.clj failed to load"
                          {:plugin (:name metadata)
                           :dir (.getPath dir)
                           :init-file init-path
                           :cause-message (ex-message t)}
                          t))))
      (let [recorded (assoc metadata
                            :source :local
                            :dir (.getPath dir)
                            :init-file init-path
                            :loaded-at (str (Instant/now)))]
        (swap! (plugin-registry runtime) assoc (:name recorded) recorded)
        recorded))))

(defn- validated-query-entry [[query-name query-def]]
  [(query/canonical-query-name query-name)
   (query/validate-query-def! query-def)])

(defn register-query [runtime query-name query-def]
  (let [entry (validated-query-entry [query-name query-def])]
    (swap! (query-registry runtime) conj entry)
    (into {} [entry])))

(defn load-queries [runtime query-defs]
  (let [validated-query-defs (into {} (map validated-query-entry) query-defs)]
    (swap! (query-registry runtime) merge validated-query-defs)
    validated-query-defs))

(defn- current-runtime []
  (or @runtime/current-runtime
      (throw (ex-info "No daemon runtime is active" {}))))

(defn register-query! [query-name query-def]
  (register-query (current-runtime) query-name query-def))

(defn load-queries! [query-defs]
  (load-queries (current-runtime) query-defs))

(defn queries [runtime]
  (into (sorted-map) @(query-registry runtime)))

(defn resolve-query [runtime query-name]
  (query/query-def @(query-registry runtime) query-name))

(defn init [runtime]
  (db/init! (ds runtime))
  {:database "initialized"})

(defn add [runtime task]
  (normalize (db/add-task! (ds runtime) task)))

(defn- apply-edges! [tx id edges]
  (doseq [{:keys [to type attributes]} edges]
    (when-not (db/get-task tx to)
      (throw (ex-info "Edge target task not found" {:to to :type type})))
    (db/add-edge! tx {:from id :to to :type type :attributes (or attributes {})})))

(defn update [runtime id patch]
  (let [{:keys [title status attributes edges]} patch]
    (jdbc/with-transaction [tx (ds runtime)]
      (when-not (db/get-task tx id)
        (throw (ex-info "Task not found" {:task-id id})))
      (apply-edges! tx id edges)
      (normalize (db/update-task! tx id {:title title
                                         :status status
                                         :attributes attributes})))))

(defn show [runtime id]
  (normalize (db/get-task (ds runtime) id)))

(defn list
  ([runtime]
   (normalize (db/all-tasks (ds runtime))))
  ([runtime query-def params]
   (normalize (db/all-tasks (ds runtime) query-def params))))

(defn list-query [runtime query-name params]
  (list runtime (resolve-query runtime query-name) params))

(defn ready
  ([runtime]
   (normalize (db/ready-tasks (ds runtime))))
  ([runtime query-def params]
   (normalize (db/ready-tasks (ds runtime) query-def params))))

(defn ready-query [runtime query-name params]
  (ready runtime (resolve-query runtime query-name) params))
