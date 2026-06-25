(ns todo.daemon.config
  (:require [clojure.java.io :as io]))

(defn- env-or [k fallback]
  (let [v (System/getenv k)]
    (if (seq v) v fallback)))

(defn- user-home []
  (System/getProperty "user.home"))

(defn- canonical-path [file]
  (.getCanonicalPath (io/file file)))

(defn- world-map [config-dir state-dir data-dir]
  {:config-dir config-dir
   :state-dir state-dir
   :data-dir data-dir
   :config-file (str config-dir "/config.json")
   :db-path (str data-dir "/tasks.sqlite")})

(defn world
  ([]
   (let [home (user-home)
         config-home (env-or "XDG_CONFIG_HOME" (str home "/.config"))
         state-home (env-or "XDG_STATE_HOME" (str home "/.local/state"))
         data-home (env-or "XDG_DATA_HOME" (str home "/.local/share"))]
     (world-map (str config-home "/atom")
                (str state-home "/atom")
                (str data-home "/atom"))))
  ([config-dir]
   (if config-dir
     (let [dir (canonical-path config-dir)]
       (world-map dir (str dir "/state") (str dir "/data")))
     (world))))
