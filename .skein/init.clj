(require '[skein.runtime.alpha :as runtime])

(runtime/sync!)
(runtime/use! :skein/libs-ephemeral
  {:ns 'skein.libs.ephemeral
   :call 'skein.libs.ephemeral/install!})
(runtime/use! :config
  {:file "config.clj"
   :after [:skein/libs-ephemeral]
   :call 'config/install!})
