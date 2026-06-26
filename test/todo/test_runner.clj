(ns todo.test-runner
  (:require [clojure.test :as test]
            [todo.alpha-test]
            [todo.cli-test]
            [todo.client-test]
            [todo.daemon-test]
            [todo.db-test]
            [todo.libs-test]
            [todo.plugin-test]
            [todo.repl-test]
            [todo.runtime-deps-test]))

(defn -main [& _]
  (let [{:keys [fail error]} (test/run-tests 'todo.alpha-test 'todo.cli-test 'todo.client-test 'todo.daemon-test 'todo.db-test 'todo.libs-test 'todo.plugin-test 'todo.repl-test 'todo.runtime-deps-test)]
    (when (pos? (+ fail error))
      (System/exit 1))))
