(ns skein.libs.alpha
  "Compatibility shim for the privileged runtime loader/config helper API.

  New config and code should require `skein.runtime.alpha`. This namespace remains
  during alpha migration so existing `skein.libs.alpha` config keeps working."
  (:refer-clojure :exclude [sync use])
  (:require [skein.runtime.alpha :as runtime]))

(defn approved
  "Return the normalized approved library roots for the selected weaver config dir.

  Compatibility wrapper for `skein.runtime.alpha/approved`."
  []
  (runtime/approved))

(defn sync!
  "Load approved local roots into the selected weaver runtime.

  Compatibility wrapper for `skein.runtime.alpha/sync!`."
  []
  (runtime/sync!))

(defn syncs
  "Return the selected weaver runtime's most recent approved-root sync state.

  Compatibility wrapper for `skein.runtime.alpha/syncs`."
  []
  (runtime/syncs))

(defn reload!
  "Reload startup files from the selected config dir in the active weaver.

  Compatibility wrapper for `skein.runtime.alpha/reload!`."
  []
  (runtime/reload!))

(defn use!
  "Activate a weaver-side module and record its use state.

  Compatibility wrapper for `skein.runtime.alpha/use!`."
  [key opts]
  (runtime/use! key opts))

(defn uses
  "Return the selected weaver runtime's module-use registry as data-first maps.

  Compatibility wrapper for `skein.runtime.alpha/uses`."
  []
  (runtime/uses))

(defn use
  "Return one module-use registry entry from the selected weaver runtime by key.

  Compatibility wrapper for `skein.runtime.alpha/use`."
  [key]
  (runtime/use key))
