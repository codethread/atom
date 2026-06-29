(ns skein.views.alpha
  "Public helper API for registering, inspecting, and invoking weaver views.

  Calls route directly when executing inside a weaver runtime, otherwise through
  the connected helper REPL world. The weaver API owns view validation, function
  resolution, registry state, and invocation."
  (:require [skein.client :as client]
            [skein.repl :as repl]
            [skein.weaver.api :as api]
            [skein.weaver.runtime :as runtime]))

(defn- call-daemon [op & args]
  (if-let [rt @runtime/current-runtime]
    (case op
      :register-view! (apply api/register-view! rt args)
      :view! (apply api/view! rt args)
      :views (api/views rt))
    (apply client/call-world (repl/connected-config-dir) {} op args)))

(defn register-view!
  "Register a weaver-memory view name to a fully qualified weaver-resolvable function symbol.

  Duplicate names replace prior registrations. When called inside the weaver JVM,
  registers directly on the active weaver runtime. When called from a connected
  helper REPL, routes to the selected weaver world from `skein.repl/connect!` /
  `strand weaver repl`; connected users should register functions that are already
  loadable in the weaver JVM."
  [name fn-sym]
  (call-daemon :register-view! name fn-sym))

(defn view!
  "Invoke a registered weaver-side view with params through the selected weaver runtime.

  The weaver resolves the registered function symbol and calls it with
  `{:params params}`. Routes directly through the weaver runtime or the connected
  helper REPL world."
  [name params]
  (call-daemon :view! name params))

(defn views
  "Return serializable weaver-memory view registry entries through the selected weaver runtime.

  Routes directly through the weaver runtime or the connected helper REPL world."
  []
  (call-daemon :views))
