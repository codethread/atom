(ns todo.specs
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]))

(defn non-blank-string? [x]
  (and (string? x) (not (str/blank? x))))

(s/def ::id non-blank-string?)
(s/def ::from ::id)
(s/def ::to ::id)
(s/def ::type non-blank-string?)
(s/def ::title non-blank-string?)
(s/def ::edge-type non-blank-string?)
(s/def ::attr-key keyword?)
(s/def ::cli-attr-value string?)
(s/def ::cli-attributes (s/map-of ::attr-key ::cli-attr-value))
(s/def ::attributes (s/nilable map?))
(s/def ::format #{"human" "edn" "json"})
(s/def ::db non-blank-string?)
(s/def ::opts (s/keys :req-un [::db ::format]))

(s/def ::add-command (s/cat :id ::id :title ::title :attrs (s/* string?)))
(s/def ::link-command (s/cat :from ::id :to ::id :type ::edge-type :attrs (s/* string?)))
(s/def ::one-id-command (s/cat :id ::id))
(s/def ::empty-command (s/cat))
(s/def ::by-attr-command (s/cat :key non-blank-string? :value string?))

(s/def ::task-input (s/keys :req-un [::id ::title] :opt-un [::attributes]))
(s/def ::edge-input (s/keys :req-un [::from ::to ::type] :opt-un [::attributes]))
