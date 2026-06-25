(ns todo.plugin-test
  (:require [clojure.test :refer [deftest is]]
            [atom.libs.alpha :as libs]
            [todo.client :as client]
            [todo.daemon.api]
            [todo.daemon.config :as daemon-config]
            [todo.daemon.runtime :as runtime]
            [todo.db-test :as db-test]))

(defn with-runtime [f]
  (let [db-file (db-test/temp-db-file)
        config-dir (str "/tmp/td-" (java.util.UUID/randomUUID))]
    (.mkdirs (java.io.File. config-dir))
    (let [rt (runtime/start! db-file {:world (daemon-config/world config-dir)})]
      (try
        (f)
        (finally
          (runtime/stop! rt)
          (db-test/delete-sqlite-family! db-file))))))

(deftest old-plugin-and-bootstrap-surfaces-are-not-available
  (is (thrown? java.io.FileNotFoundException (require 'atom.plugin.alpha)))
  (is (thrown? java.io.FileNotFoundException (require 'atom.bootstrap.alpha)))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Unknown daemon API operation"
                        (client/fixed-form :load-plugin ["plugins/demo"])))
  (is (nil? (ns-resolve 'todo.daemon.api 'load-plugin)))
  (is (nil? (ns-resolve 'todo.daemon.api 'plugins)))
  (is (nil? (ns-resolve 'todo.daemon.api 'plugin))))

(deftest library-workspace-state-is-the-public-path
  (with-runtime
    (fn []
      (is (= {:libs {}} (libs/approved)))
      (is (= {:libs {}} (libs/syncs)))
      (is (= {} (libs/uses))))))

(deftest prelude-is-opt-in-and-exposes-library-workspace-conveniences
  (with-runtime
    (fn []
      (require 'atom.prelude.alpha)
      (let [prelude-approved (requiring-resolve 'atom.prelude.alpha/approved)
            prelude-syncs (requiring-resolve 'atom.prelude.alpha/syncs)
            prelude-uses (requiring-resolve 'atom.prelude.alpha/uses)
            prelude-use! (requiring-resolve 'atom.prelude.alpha/use!)]
        (is (ifn? prelude-approved))
        (is (ifn? prelude-syncs))
        (is (ifn? prelude-uses))
        (is (ifn? prelude-use!))
        (is (= {:libs {}} (prelude-approved)))
        (is (= {:libs {}} (prelude-syncs)))
        (is (= {} (prelude-uses)))
        (is (nil? (ns-resolve 'atom.prelude.alpha 'use-defaults!)))
        (is (nil? (ns-resolve 'atom.prelude.alpha 'load-plugin!)))
        (is (nil? (ns-resolve 'atom.prelude.alpha 'plugins)))
        (is (nil? (ns-resolve 'atom.prelude.alpha 'plugin)))))))
