(ns skein.events.alpha
  "Public helper API for registering and inspecting weaver event handlers.

  Calls route directly when executing inside a weaver runtime, otherwise through
  the connected helper REPL world. The weaver API owns event handler validation,
  function resolution, registry state, and asynchronous failure capture."
  (:require [skein.client :as client]
            [skein.repl :as repl]
            [skein.weaver.api :as api]
            [skein.weaver.runtime :as runtime]))

(defn- call-daemon [op & args]
  (if-let [rt @runtime/current-runtime]
    (case op
      :event-handlers (api/event-handlers rt)
      :recent-event-failures (api/recent-event-failures rt)
      :register-event-handler! (apply api/register-event-handler! rt args)
      :unregister-event-handler! (apply api/unregister-event-handler! rt args))
    (apply client/call-world (repl/connected-config-dir) {} op args)))

(defn register!
  "Register or replace an event handler in the selected weaver runtime.

  `key` is a stable keyword/symbol/string, `types` is a non-empty set of event
  type keywords, and `fn-sym` is a fully qualified function symbol resolvable in
  the weaver JVM. Optional `metadata` must be data-first. The handler receives
  one event map."
  ([key types fn-sym]
   (register! key types fn-sym {}))
  ([key types fn-sym metadata]
   (call-daemon :register-event-handler! key types fn-sym metadata)))

(defn unregister!
  "Unregister an event handler by stable key."
  [key]
  (call-daemon :unregister-event-handler! key))

(defn handlers
  "Return data-first event handler registry entries."
  []
  (call-daemon :event-handlers))

(defn recent-failures
  "Return recent event handler failures as data-first maps."
  []
  (call-daemon :recent-event-failures))
