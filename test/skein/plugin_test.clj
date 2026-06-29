(ns skein.plugin-test
  (:require [clojure.test :refer [deftest is]]
            [skein.libs.alpha :as libs]
            [skein.weaver.api]
            [skein.weaver.config :as daemon-config]
            [skein.weaver.runtime :as runtime]
            [skein.db-test :as db-test]))
(defn test-world [config-dir]
  (daemon-config/world config-dir
                       (str config-dir "/state")
                       (str config-dir "/data")))


(defn with-runtime [f]
  (let [db-file (db-test/temp-db-file)
        config-dir (str "/tmp/td-" (java.util.UUID/randomUUID))]
    (.mkdirs (java.io.File. config-dir))
    (let [rt (runtime/start! db-file {:world (test-world config-dir)})]
      (try
        (f)
        (finally
          (runtime/stop! rt)
          (db-test/delete-sqlite-family! db-file))))))

(deftest old-plugin-and-bootstrap-surfaces-are-not-available
  (is (thrown? java.io.FileNotFoundException (require 'atom.plugin.alpha)))
  (is (thrown? java.io.FileNotFoundException (require 'atom.bootstrap.alpha)))
  (is (thrown? java.io.FileNotFoundException (require 'atom.prelude.alpha)))
  (is (nil? (ns-resolve 'skein.client 'load-plugin)))
  (is (nil? (ns-resolve 'skein.weaver.api 'load-plugin)))
  (is (nil? (ns-resolve 'skein.weaver.api 'plugins)))
  (is (nil? (ns-resolve 'skein.weaver.api 'plugin))))

(deftest library-workspace-state-is-the-public-path
  (with-runtime
    (fn []
      (is (= {:libs {}} (libs/approved)))
      (is (= {:libs {}} (libs/syncs)))
      (is (= {} (libs/uses))))))

