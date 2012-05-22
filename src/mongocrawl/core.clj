(ns mongocrawl.core
  (:require [tentacles.users :as users])
  (:require [tentacles.repos :as repos])
  (:use [expectations])
  (:require [clojure.pprint :as pp]))

(defn reduced-user [u]
  (let [relevant-fields [:followers
                         :name
                         :location
                         :login
                         :public_repos]]
    (zipmap relevant-fields
            (map u relevant-fields))))

(def clojure (repos/specific-repo "clojure" "clojure"))

(def visited-people (ref #{}))
(def visited-repos (ref #{}))

(def clojure-collabs
  (map :login (repos/collaborators "clojure" "clojure")))

(defn repos-of-user [u]
  (map :name (repos/user-repos u)))

(defn users-of-repo [u r]
  (map :login (repos/collaborators u r)))

(defn step [users repos]
  )

(defn update-users [users repos]
  (let [all-users (set (apply concat (vals repos)))
        new-names (remove users all-users)
        new-repos (map repos-of-user new-names)
        new-users (zipmap new-names new-repos)]
    (merge users new-users)))

(defn update-repos [users repos]
  (let [all-repos 1];(set (map #([(apply concat (vals users)))]
    ))

(println (update-users {"mrocklin" ["sympy"]}
                       {["seed" "seed"]
                        ["mrocklin" "eigenhombre"]}))

