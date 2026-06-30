(ns config.core
  "Repo-local Skein runtime configuration for skein-src."
  (:require [skein.libs.ephemeral :as ephemeral]))

(defn install!
  "Install repo-local Skein runtime configuration.

  This config library depends on the blessed `skein.libs.ephemeral` helpers so
  repo-local weavers can activate userland ephemeral strand helpers before
  loading additional project configuration."
  []
  {:installed true
   :namespace 'config.core
   :ephemeral {:namespace 'skein.libs.ephemeral
               :creator 'skein.libs.ephemeral/ephemeral!
               :burner 'skein.libs.ephemeral/burn-ephemeral!
               :query ephemeral/ephemeral-query}})
