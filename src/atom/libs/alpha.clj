(ns atom.libs.alpha
  (:refer-clojure :exclude [sync])
  (:require [todo.client :as client]
            [todo.daemon.runtime :as runtime]
            [todo.repl :as repl]))

(defn- call-daemon [op & args]
  (if-let [rt @runtime/current-runtime]
    (apply (requiring-resolve (symbol "todo.daemon.api" (name op))) rt args)
    (apply client/call-world (repl/connected-config-dir) {} op args)))

(defn approved
  "Return normalized approved library config from the selected daemon config-dir libs.edn."
  []
  (call-daemon :approved-libs))

(defn sync!
  "Sync approved local roots into the selected daemon runtime and return per-library results."
  []
  (call-daemon :sync-approved-libs))

(defn syncs
  "Return daemon-lifetime approved library sync state."
  []
  (call-daemon :approved-lib-syncs))
