(ns atom.prelude.alpha
  "Optional interactive conveniences for trusted Atom library-workspace workflows."
  (:refer-clojure :exclude [sync use])
  (:require [atom.libs.alpha :as libs]))

(def approved libs/approved)
(def sync! libs/sync!)
(def syncs libs/syncs)
(def use! libs/use!)
(def uses libs/uses)
(def use libs/use)
