(ns skein.libs.ephemeral
  "Userland helpers for temporary, parent-owned work strands.

  This namespace is intentionally authorable example code: it composes the
  documented `skein.repl` and `skein.graph.alpha` helper surfaces and owns no
  privileged loader/config/runtime implementation."
  (:require [skein.graph.alpha :as graph]
            [skein.repl :as repl]))

(defn ephemeral!
  "Create a userland ephemeral strand under parent-id.

  This uses userland attributes, not core :ephemeral lifecycle. The strand is
  persistent with `:attr ephemeral true` and a parent-of edge from the parent.
  It can be burned later with `burn-ephemeral!`."
  ([parent-id title]
   (ephemeral! parent-id title {}))
  ([parent-id title attributes]
   (let [strand (repl/strand! title (merge {:ephemeral "true"} attributes))]
     (repl/update! parent-id {:edges [{:type "parent-of" :to (:id strand)}]})
     strand)))

(def ephemeral-query
  "Query form selecting active userland ephemeral strands."
  [:and [:= [:attr :ephemeral] "true"] [:= :state "active"]])

(defn ephemeral-ids
  "Return active userland ephemeral strand ids."
  ([]
   (ephemeral-ids {}))
  ([_opts]
   (graph/query-ids! ephemeral-query {})))

(defn burn-ephemeral!
  "Burn all active userland ephemeral strands."
  []
  (let [ids (ephemeral-ids)]
    (if (seq ids)
      (graph/burn-by-ids! ids)
      {:burned [] :count 0})))

(defn install!
  "Install ephemeral strand helpers into the active weaver."
  []
  {:installed true
   :namespace 'skein.libs.ephemeral
   :ephemeral {:attribute :ephemeral
               :creator 'skein.libs.ephemeral/ephemeral!
               :burner 'skein.libs.ephemeral/burn-ephemeral!}})
