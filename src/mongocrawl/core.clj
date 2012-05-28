(ns mongocrawl.core
  (:require [tentacles.users :as users])
  (:require [tentacles.repos :as repos])
  (:require [mongocrawl.gitrequest :as gitrequest])
  (:use [clojure.data.priority-map :as priority-map])
  (:use [expectations])
  (:require [clojure.pprint :as pp]))

(defn reduced-user [user]
  (let [relevant-fields [:followers
                         :name
                         :location
                         :login
                         :public_repos]]
    (zipmap relevant-fields
            (map user relevant-fields))))

(defn priority-user [user]
  (min (:followers user) (:public_repos user)))
(defn priority-repo [repo]
  (:forks repo))

(defn rekey [login repo-names]
  (for [repo-name repo-names] [login repo-name]))

; higher priorities first
(def user-queue (atom (priority-map-by (comparator >)))) 
(def repo-queue (atom (priority-map-by (comparator >))))

(def user-graph (atom {}))
(def repo-graph (atom {}))

(defn expand-users
  ([login]
     (if-not (contains? @user-graph login)
       (let [repos (gitrequest/user-repos login)
             reponames (map :name repos)
             new-repos (remove
                         (fn [r] (contains? @repo-graph [login (:name r)]))
                         repos)         
             new-reponames (map :name new-repos)
             new-userrepos (rekey login new-reponames)
             new-priorities (map priority-repo new-repos)]
         (swap! user-graph assoc login reponames)
         (swap! repo-queue into (map vector new-userrepos new-priorities )))))
  ([]
   (expand-users (first (peek @user-queue)))
   (swap! user-queue pop )))


(defn expand-repos
  ([login repo-name]
     (if-not (contains? @repo-graph [login repo-name])
       (let [collabs (gitrequest/collaborators login repo-name)
             collab-logins (map :login collabs)
             new-collab-logins (remove 
                                (fn [login] (contains? @user-graph login)) 
                                collab-logins)
             new-collaborators (map gitrequest/user new-collab-logins)
             new-priorities (map priority-user new-collaborators)]
         (swap! repo-graph assoc [login repo-name] collab-logins)
         (swap! user-queue into 
                (map vector new-collab-logins new-priorities)))))
  ([[login repo-name]] (expand-repos login repo-name))
  ([]
   (expand-repos (first (peek @repo-queue)))
   (swap! repo-queue pop)))
