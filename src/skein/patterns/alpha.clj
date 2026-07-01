(ns skein.patterns.alpha
  "Public helper API for registering, inspecting, and invoking weave patterns.

  Calls route directly when executing inside a weaver runtime, otherwise through
  an explicit connected client world. The weaver API owns pattern validation,
  function resolution, input spec validation, and transactional batch creation."
  (:require [skein.client :as client]
            [skein.weaver.api :as api]
            [skein.weaver.runtime :as runtime]))

(defn- call-daemon [op & args]
  (if-let [rt @runtime/current-runtime]
    (case op
      :patterns (api/patterns rt)
      :pattern-explain (apply api/pattern-explain rt args)
      :register-pattern! (apply api/register-pattern! rt args)
      :resolve-pattern (apply api/resolve-pattern rt args)
      :weave! (apply api/weave! rt args))
    (apply client/call-world ((requiring-resolve 'skein.repl/connected-config-dir)) ((requiring-resolve 'skein.repl/connected-opts)) op args)))

(defn register-pattern!
  "Register a weaver-memory weave pattern.

  Pattern names are simple names. Optional `doc` is a non-blank string.
  `fn-sym` must be a fully qualified function symbol loadable in the weaver JVM.
  `input-spec` is a clojure.spec name used for pre-invocation validation and
  caller explanation. Routes directly when called inside the weaver JVM, or
  through an explicit connected client world."
  ([name fn-sym input-spec]
   (call-daemon :register-pattern! name fn-sym input-spec))
  ([name doc fn-sym input-spec]
   (call-daemon :register-pattern! name doc fn-sym input-spec)))

(defn patterns
  "Return serializable weaver-memory pattern registry entries."
  []
  (call-daemon :patterns))

(defn pattern
  "Return one registered pattern entry, or fail loudly if missing."
  [name]
  (call-daemon :resolve-pattern name))

(defn explain
  "Return caller guidance for a registered pattern's input contract."
  [name]
  (call-daemon :pattern-explain name))

(defn weave!
  "Invoke a registered pattern with input data and atomically create its strand batch."
  [name input]
  (call-daemon :weave! name input))
