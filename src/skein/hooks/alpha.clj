(ns skein.hooks.alpha
  "Public helper API for registering and inspecting weaver lifecycle hooks.

  Calls route directly when executing inside a weaver runtime, otherwise through
  an explicit connected client world. The weaver API owns hook validation, function
  resolution, registry state, and synchronous invocation by later lifecycle gates."
  (:require [skein.client :as client]
            [skein.repl :as repl]
            [skein.weaver.api :as api]
            [skein.weaver.runtime :as runtime]))

(defn- call-daemon [op & args]
  (if-let [rt @runtime/current-runtime]
    (case op
      :hooks (api/hooks rt)
      :register-hook! (apply api/register-hook! rt args)
      :unregister-hook! (apply api/unregister-hook! rt args))
    (apply client/call-world (repl/connected-config-dir) (repl/connected-opts) op args)))

(defn register!
  "Register or replace a lifecycle hook in the selected weaver runtime.

  `key` is a stable keyword/symbol/string, `types` is a non-empty set of hook
  type keywords, and `fn-sym` is a fully qualified function symbol resolvable in
  the weaver JVM. Optional `opts` must be data-first and may include integer
  `:order`; remaining keys are metadata for introspection. The hook receives one
  context map when invoked by lifecycle gates."
  ([key types fn-sym]
   (register! key types fn-sym {}))
  ([key types fn-sym opts]
   (call-daemon :register-hook! key types fn-sym opts)))

(defn unregister!
  "Unregister a lifecycle hook by stable key."
  [key]
  (call-daemon :unregister-hook! key))

(defn hooks
  "Return data-first lifecycle hook registry entries in execution order."
  []
  (call-daemon :hooks))
