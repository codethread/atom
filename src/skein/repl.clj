(ns skein.repl
  "Interactive helper API for connected Skein weaver workflows.

  This namespace is preloaded by `strand weaver repl` and exposes the compact
  trusted Clojure surface for strand, query, relation, and pattern operations
  against the selected weaver world. Helpers fail loudly until `connect!` selects
  a world."
  (:require [clojure.main :as main]
            [clojure.string :as str]
            [skein.client :as client]
            [skein.weaver.config :as daemon-config]
            [skein.patterns.alpha :as patterns-alpha]
            [skein.query :as query]))

(def ^:private no-connection ::no-connection)
(def ^:private default-world ::default-world)
(defonce ^:private active-config-dir (atom no-connection))

(defn connected-config-dir
  "Return the selected weaver config directory for connected helper routing.

  Returns nil when connected to the default world. Throws with remediation when
  no helper connection has been selected. Alpha helper namespaces use this to
  route calls from connected REPL clients to the active weaver."
  []
  (case @active-config-dir
    ::no-connection (throw (ex-info "No Skein weaver world is connected. Start a connected helper REPL with `strand weaver repl`, or call (connect!) / (connect! \"/path/to/config-dir\") before using skein.repl helpers."
                                   {:helper 'connect!}))
    ::default-world nil
    @active-config-dir))

(defn- config-dir []
  (connected-config-dir))

(defn connect!
  "Select the active weaver world for helper calls.

  With no arguments, connects to the default world. With `config-dir`, connects
  to that explicit daemon config directory. Fails loudly if given a database file
  or if the selected weaver world is not reachable. Returns the normalized config
  directory path for the selected world."
  ([]
   (connect! nil))
  ([config-dir]
   (reset! active-config-dir no-connection)
   (when (and config-dir (.isFile (java.io.File. config-dir)))
     (throw (ex-info "connect! expects a daemon config directory, not a database file" {:config-dir config-dir})))
   (let [world (daemon-config/world config-dir)]
     (if config-dir
       (do
         (client/status-world (:config-dir world))
         (reset! active-config-dir (:config-dir world)))
       (do
         (client/status-world nil)
         (reset! active-config-dir default-world)))
     (:config-dir world))))

(declare call-daemon)

(defn- daemon [op & args]
  (let [dir (config-dir)]
    (call-daemon #(apply client/call-world dir {} op args))))

(defn init!
  "Initialize the active weaver store schema."
  []
  (daemon :init))

(def ^:private strand-core-keys #{:title :state :attributes :edges})

(defn- reject-core-attribute-keys! [attributes]
  (when (map? attributes)
    (let [core-keys (seq (filter strand-core-keys (keys attributes)))]
      (when core-keys
        (throw (ex-info "Two-argument strand! treats the second argument as attributes; pass lifecycle fields as the third argument"
                        {:keys (vec core-keys)})))))
  attributes)

(defn strand!
  "Create a strand in the active weaver world.

  Accepts a title, optional attributes map, and optional lifecycle/options map
  such as `{:state \"closed\"}` as the third argument. The two-argument form is
  attributes-only and rejects core strand fields instead of treating them as
  attributes. Returns the created normalized strand row."
  ([title]
   (strand! title {} {}))
  ([title attributes]
   (strand! title (reject-core-attribute-keys! attributes) {}))
  ([title attributes lifecycle]
   (daemon :add (merge {:title title :attributes attributes} lifecycle))))

(defn update!
  "Apply a patch to an existing strand.

  The patch may include supported core fields such as `:title`, `:state`,
  `:attributes`, and `:edges`; invalid update fields fail in the weaver. Returns
  the weaver-normalized update result."
  ([id patch]
   (daemon :update id patch)))

(defn supersede!
  "Replace one strand with another through the weaver supersession operation.

  Marks `old-id` replaced, records the supersession edge from replacement to old,
  rewires direct dependents, and returns the normalized supersession result."
  [old-id replacement-id]
  (daemon :supersede old-id replacement-id))

(defn strand
  "Return the normalized strand row for `id`, or nil when no such strand exists."
  [id]
  (daemon :show id))

(defn declare-acyclic-relation!
  "Declare `relation` as a durable acyclic structural relation.

  Declaration is idempotent and fails loudly if existing edges of that relation
  prevent making the acyclicity guarantee."
  [relation]
  (daemon :declare-acyclic-relation! relation))

(defn acyclic-relations
  "Return sorted relation names declared acyclic in the active world."
  []
  (daemon :acyclic-relations))

(defn burn!
  "Physically delete one or more strands and their incident edges.

  Missing ids fail loudly. Returns the weaver burn summary."
  ([id]
   (daemon :burn-by-id id))
  ([id & ids]
   (daemon :burn-by-ids (vec (cons id ids)))))

(defn burn-by-ids!
  "Physically delete all strand ids in `ids` and their incident edges.

  Missing ids fail loudly. Returns the weaver burn summary."
  [ids]
  (daemon :burn-by-ids (vec ids)))

(defn- call-daemon [f]
  (try
    (f)
    (catch clojure.lang.ExceptionInfo e
      (if-let [weaver-message (:weaver-message (ex-data e))]
        (throw (ex-info weaver-message (:weaver-data (ex-data e))))
        (throw e)))))

(defn defquery!
  "Register `query-name` to `query-def` in the active weaver query registry.

  Registry state is in-memory for the current weaver lifetime. Query definitions
  may be plain predicates or parameterized query maps. Returns the registered
  query entry."
  [query-name query-def]
  (let [dir (config-dir)]
    (call-daemon #(client/call-world dir {} :register-query query-name query-def))))

(defn load-queries!
  "Load named queries from one EDN map at `path` into the active weaver registry.

  The file must contain exactly one map of query names to query definitions.
  Returns the loaded query entries."
  [path]
  (let [dir (config-dir)
        registry (query/read-edn-file path)]
    (when-not (map? registry)
      (throw (ex-info "Query file must contain one EDN map of query names to query definitions" {:path path})))
    (call-daemon #(client/call-world dir {} :load-queries registry))))

(defn queries
  "Return the active weaver's in-memory named query registry."
  []
  (let [dir (config-dir)]
    (call-daemon #(client/call-world dir {} :queries))))

(defn- named-query? [query-or-def]
  (or (symbol? query-or-def) (keyword? query-or-def)))

(defn- run-query [dir query-or-def params ad-hoc named]
  (call-daemon #(if (named-query? query-or-def)
                  (client/call-world dir {} named query-or-def params)
                  (client/call-world dir {} ad-hoc query-or-def params))))

(defn query
  "Return strands matching an ad hoc query definition or named query.

  `query-or-def` may be a registered query symbol/keyword or a query predicate
  form. `params` supplies runtime values for parameterized queries."
  ([query-or-def]
   (query query-or-def {}))
  ([query-or-def params]
   (run-query (config-dir) query-or-def params :list :list-query)))

(defn strands
  "Return active-world strands, optionally filtered by a query.

  With no arguments, returns all strands. With a query definition or registered
  query name, delegates to `query` with optional params."
  ([]
   (daemon :list))
  ([query-or-def]
   (query query-or-def))
  ([query-or-def params]
   (query query-or-def params)))

(defn ready
  "Return active strands whose direct `depends-on` dependencies are not active.

  Optional query arguments further filter the ready set using an ad hoc predicate
  or registered query name with params."
  ([]
   (daemon :ready))
  ([query-or-def]
   (ready query-or-def {}))
  ([query-or-def params]
   (run-query (config-dir) query-or-def params :ready :ready-query)))

(defn defpattern!
  "Register a runtime pattern in the active weaver pattern registry.

  Accepts a simple pattern name, optional non-blank doc string, fully qualified
  function symbol, and input spec name. Duplicate names replace prior entries for
  the current weaver lifetime."
  ([pattern-name fn-sym input-spec]
   (patterns-alpha/register-pattern! pattern-name fn-sym input-spec))
  ([pattern-name doc fn-sym input-spec]
   (patterns-alpha/register-pattern! pattern-name doc fn-sym input-spec)))

(defn patterns
  "Return the active weaver's in-memory pattern registry."
  []
  (patterns-alpha/patterns))

(defn pattern
  "Return the registered pattern named `pattern-name`.

  Missing patterns fail loudly."
  [pattern-name]
  (patterns-alpha/pattern pattern-name))

(defn pattern-explain
  "Return serializable input guidance for the registered pattern `pattern-name`.

  Missing patterns or invalid registered specs fail loudly."
  [pattern-name]
  (patterns-alpha/explain pattern-name))

(defn weave!
  "Invoke the registered pattern `pattern-name` with `input` and create its batch.

  The weaver validates input against the pattern's registered spec and requires
  the pattern function to return a valid batch strand vector. Returns the batch
  creation result."
  [pattern-name input]
  (patterns-alpha/weave! pattern-name input))

(defn- eval-stdin! []
  (let [reader (java.io.PushbackReader. *in*)
        eof (Object.)]
    (loop []
      (let [form (read reader false eof)]
        (when-not (identical? eof form)
          (prn (eval form))
          (recur))))))

(defn -main
  "Start a connected Skein helper REPL or evaluate stdin forms.

  Usage: `skein.repl [--stdin] [config-dir]`. Interactive mode starts a plain
  Clojure REPL in this namespace. Stdin mode evaluates each top-level form in
  order, prints one result per form, and exits non-zero on evaluation failure."
  [& args]
  (let [[mode config-dir] (case (count args)
                            0 [:repl nil]
                            1 (if (= "--stdin" (first args))
                                [:stdin nil]
                                [:repl (first args)])
                            2 (if (= "--stdin" (first args))
                                [:stdin (second args)]
                                (throw (ex-info "Usage: skein.repl [--stdin] [config-dir]" {:args args})))
                            (throw (ex-info "Usage: skein.repl [--stdin] [config-dir]" {:args args})))]
    (connect! config-dir)
    (binding [*ns* (the-ns 'skein.repl)]
      (case mode
        :stdin (try
                 (eval-stdin!)
                 (catch Throwable t
                   (binding [*out* *err*]
                     (println (or (ex-message t) (str t))))
                   (System/exit 1)))
        :repl (main/repl :prompt #(print "skein=> "))))))
