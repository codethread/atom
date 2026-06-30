(require '[skein.libs.alpha :as libs])

(libs/sync!)
(libs/use! :skein/libs-ephemeral
  {:ns 'skein.libs.ephemeral
   :call 'skein.libs.ephemeral/install!})
(libs/use! :skein-src/config
  {:ns 'config.core
   :libs #{'skein-src/config}
   :after [:skein/libs-ephemeral]
   :call 'config.core/install!})
