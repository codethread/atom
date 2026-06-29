(ns skein.batch.alpha
  "Public helper API for applying batch graph mutations.

  Calls route directly when executing inside a weaver runtime, otherwise through
  the connected helper REPL world. The weaver API owns payload validation and
  transactional persistence."
  (:require [skein.client :as client]
            [skein.repl :as repl]
            [skein.weaver.api :as api]
            [skein.weaver.runtime :as runtime]))

(defn apply!
  "Apply one transactional batch graph mutation payload through the selected weaver.

  Returns the weaver API result. Fails loudly when no helper REPL world is
  connected or when the payload is invalid."
  [payload]
  (if-let [rt @runtime/current-runtime]
    (api/apply-batch rt payload)
    (client/call-world (repl/connected-config-dir) {} :apply-batch payload)))
