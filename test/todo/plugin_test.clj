(ns todo.plugin-test
  (:require [clojure.test :refer [deftest is testing]]
            [atom.bootstrap.alpha :as bootstrap]
            [atom.plugin.alpha :as plugin]
            [todo.daemon.config :as daemon-config]
            [todo.daemon.runtime :as runtime]
            [todo.db-test :as db-test]))

(defn with-runtime [f]
  (let [db-file (db-test/temp-db-file)
        config-dir (str "/tmp/td-" (java.util.UUID/randomUUID))]
    (.mkdirs (java.io.File. config-dir))
    (let [rt (runtime/start! db-file {:world (daemon-config/world config-dir)})]
      (try
        (f rt)
        (finally
          (runtime/stop! rt)
          (db-test/delete-sqlite-family! db-file))))))

(deftest plugin-registration-records-and-returns-metadata
  (with-runtime
    (fn [_]
      (is (= {:format-version 1
              :name 'demo/plugin
              :version "0.1.0"
              :requires-atom "0.0.1"
              :provides ['demo/feature]}
             (plugin/register! {:format-version 1
                                :name :demo/plugin
                                :version "0.1.0"
                                :requires-atom "0.0.1"
                                :provides [:demo/feature]}))))))

(deftest plugin-registration-replaces-by-canonical-name
  (with-runtime
    (fn [_]
      (plugin/register! {:format-version 1 :name :demo/plugin :version "0.1.0"})
      (is (= {:format-version 1 :name 'demo/plugin :version "0.2.0"}
             (plugin/register! {:format-version 1 :name 'demo/plugin :version "0.2.0"})))
      (is (= {:format-version 1 :name 'demo/plugin :version "0.2.0"}
             (plugin/plugin :demo/plugin)))
      (is (= [{:format-version 1 :name 'demo/plugin :version "0.2.0"}]
             (plugin/plugins))))))

(deftest plugin-lookup-normalizes-symbol-and-keyword-names
  (with-runtime
    (fn [_]
      (plugin/register! {:format-version 1 :name :demo/plugin})
      (is (= {:format-version 1 :name 'demo/plugin} (plugin/plugin 'demo/plugin)))
      (is (= {:format-version 1 :name 'demo/plugin} (plugin/plugin :demo/plugin)))
      (is (nil? (plugin/plugin :demo/missing))))))

(deftest plugin-introspection-is-daemon-lifetime-state
  (let [db-file (db-test/temp-db-file)
        config-dir (str "/tmp/td-" (java.util.UUID/randomUUID))]
    (.mkdirs (java.io.File. config-dir))
    (let [world (daemon-config/world config-dir)
          rt (runtime/start! db-file {:world world})]
      (try
        (plugin/register! {:format-version 1 :name :demo/a})
        (plugin/register! {:format-version 1 :name :demo/b})
        (is (= ['demo/a 'demo/b] (mapv :name (plugin/plugins))))
        (runtime/stop! rt)
        (let [fresh-rt (runtime/start! db-file {:world world})]
          (try
            (is (= [] (plugin/plugins)))
            (finally
              (runtime/stop! fresh-rt))))
        (finally
          (db-test/delete-sqlite-family! db-file))))))

(deftest plugin-metadata-validation-fails-loudly
  (with-runtime
    (fn [_]
      (testing "unknown key rejection"
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"unknown keys"
                              (plugin/register! {:format-version 1 :name :demo/plugin :extra true}))))
      (testing "format-version validation"
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"format version"
                              (plugin/register! {:format-version 2 :name :demo/plugin})))
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #":format-version"
                              (plugin/register! {:name :demo/plugin}))))
      (testing "invalid metadata"
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"metadata must be a map"
                              (plugin/register! nil)))
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"symbol or keyword"
                              (plugin/register! {:format-version 1 :name "demo/plugin"})))
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #":version must be a string"
                              (plugin/register! {:format-version 1 :name :demo/plugin :version 1})))
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #":requires-atom must be a string"
                              (plugin/register! {:format-version 1 :name :demo/plugin :requires-atom 1})))
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #":provides must be a vector"
                              (plugin/register! {:format-version 1 :name :demo/plugin :provides :demo/feature})))
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"unknown keys"
                              (plugin/register! {:format-version 1 :name :demo/plugin :source :local})))))))

(defn write-plugin! [dir metadata init-body]
  (.mkdirs (java.io.File. dir))
  (spit (java.io.File. dir "atom-plugin.edn") metadata)
  (spit (java.io.File. dir "init.clj") init-body))

(deftest load-plugin-loads-local-directory-and-registers-metadata
  (with-runtime
    (fn [rt]
      (let [config-dir (get-in rt [:metadata :config-dir])
            plugin-dir (str config-dir "/plugins/demo")
            marker (str config-dir "/loaded.txt")]
        (write-plugin! plugin-dir
                       "{:format-version 1 :name :demo/plugin :version \"0.1.0\" :provides [:demo/feature]}"
                       (str "(spit " (pr-str marker) " \"loaded\")"))
        (let [recorded (plugin/load-plugin! "plugins/demo")]
          (is (= 'demo/plugin (:name recorded)))
          (is (= "0.1.0" (:version recorded)))
          (is (= ['demo/feature] (:provides recorded)))
          (is (= :local (:source recorded)))
          (is (= (.getCanonicalPath (java.io.File. plugin-dir)) (:dir recorded)))
          (is (= (.getCanonicalPath (java.io.File. plugin-dir "init.clj")) (:init-file recorded)))
          (is (string? (:loaded-at recorded)))
          (is (= "loaded" (slurp marker)))
          (is (= recorded (plugin/plugin :demo/plugin))))))))

(deftest load-plugin-accepts-absolute-plugin-path
  (with-runtime
    (fn [rt]
      (let [config-dir (get-in rt [:metadata :config-dir])
            plugin-dir (str config-dir "/absolute-demo")]
        (write-plugin! plugin-dir "{:format-version 1 :name :demo/absolute}" "nil")
        (is (= 'demo/absolute (:name (plugin/load-plugin! (.getAbsolutePath (java.io.File. plugin-dir))))))))))

(deftest load-plugin-fails-loudly-before-registration
  (with-runtime
    (fn [rt]
      (let [config-dir (get-in rt [:metadata :config-dir])
            missing-dir "missing-plugin"
            no-metadata (str config-dir "/no-metadata")
            no-init (str config-dir "/no-init")
            malformed (str config-dir "/malformed")
            unknown (str config-dir "/unknown")
            loader-owned (str config-dir "/loader-owned")
            bad-version (str config-dir "/bad-version")
            throwing (str config-dir "/throwing")
            invalid-marker (str config-dir "/invalid-loaded.txt")]
        (.mkdirs (java.io.File. no-metadata))
        (.mkdirs (java.io.File. no-init))
        (spit (java.io.File. no-init "atom-plugin.edn") "{:format-version 1 :name :demo/no-init}")
        (write-plugin! malformed "{:format-version" "nil")
        (write-plugin! unknown "{:format-version 1 :name :demo/unknown :extra true}" (str "(spit " (pr-str invalid-marker) " \"loaded\")"))
        (write-plugin! loader-owned "{:format-version 1 :name :demo/loader-owned :source :local}" (str "(spit " (pr-str invalid-marker) " \"loaded\")"))
        (write-plugin! bad-version "{:format-version 2 :name :demo/bad-version}" (str "(spit " (pr-str invalid-marker) " \"loaded\")"))
        (write-plugin! throwing "{:format-version 1 :name :demo/throwing}" "(throw (ex-info \"boom\" {}))")
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"directory" (plugin/load-plugin! missing-dir)))
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"metadata file is missing" (plugin/load-plugin! no-metadata)))
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"init.clj is missing" (plugin/load-plugin! no-init)))
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"metadata is malformed" (plugin/load-plugin! malformed)))
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"unknown keys" (plugin/load-plugin! unknown)))
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"unknown keys" (plugin/load-plugin! loader-owned)))
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"format version" (plugin/load-plugin! bad-version)))
        (is (not (.exists (java.io.File. invalid-marker))))
        (is (thrown? Throwable (plugin/load-plugin! throwing)))
        (is (nil? (plugin/plugin :demo/throwing)))))))

(deftest bootstrap-and-prelude-namespaces-load
  (with-runtime
    (fn [_]
      (is (find-ns 'atom.bootstrap.alpha))
      (let [prelude-before (find-ns 'atom.prelude.alpha)
            state (bootstrap/use-defaults!)
            reloaded (bootstrap/use-defaults!)]
        (is (= ['atom.bootstrap.alpha 'atom.plugin.alpha]
               (mapv :name (:plugins state))))
        (is (= (:plugins state) (:plugins reloaded)))
        (is (= (set (map :name (:plugins state)))
               (set (map :name (:registered reloaded)))))
        (is (some #(= 'atom/plugin-helpers %) (:provides (plugin/plugin 'atom.plugin.alpha))))
        (is (identical? prelude-before (find-ns 'atom.prelude.alpha)))))))

(deftest prelude-is-opt-in-and-exposes-plugin-helper-conveniences
  (with-runtime
    (fn [_]
      (require 'atom.prelude.alpha)
      (let [prelude-register! (requiring-resolve 'atom.prelude.alpha/register!)
            prelude-plugin (requiring-resolve 'atom.prelude.alpha/plugin)
            prelude-plugins (requiring-resolve 'atom.prelude.alpha/plugins)
            prelude-load-plugin! (requiring-resolve 'atom.prelude.alpha/load-plugin!)]
        (is (ifn? prelude-register!))
        (is (ifn? prelude-plugin))
        (is (ifn? prelude-plugins))
        (is (ifn? prelude-load-plugin!))
        (is (= {:format-version 1 :name 'demo/prelude}
               (prelude-register! {:format-version 1 :name :demo/prelude})))
        (is (= {:format-version 1 :name 'demo/prelude}
               (prelude-plugin :demo/prelude)))
        (is (= [{:format-version 1 :name 'demo/prelude}]
               (prelude-plugins)))))))
