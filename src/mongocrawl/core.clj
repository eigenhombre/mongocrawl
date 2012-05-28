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


(defn expand-users
  ([{user-graph :user-graph repo-graph :repo-graph 
     repo-queue :repo-queue user-queue :user-queue}
    login]
     (if (contains? user-graph login)
       {:user-graph user-graph  :user-queue user-queue
        :repo-graph repo-graph  :repo-queue repo-queue}
       (let [repos (gitrequest/user-repos login)
             new-repos (remove
                         (fn [r] (contains? repo-graph [login (:name r)]))
                         repos)         
             new-reponames (map :name new-repos)
             new-userrepos (rekey login new-reponames)
             new-priorities (map priority-repo new-repos)
             new-priority-pairs (map vector new-userrepos new-priorities)
             new-user-graph (assoc user-graph login (map :name repos))
             new-repo-queue (into repo-queue new-priority-pairs)]
         {:user-graph new-user-graph :user-queue user-queue
          :repo-graph repo-graph     :repo-queue new-repo-queue}
         )))
  ([state]
   (if (empty? (state :user-queue))
     state
     (let [login (-> state :user-queue peek first)
           new-state (update-in state [:user-queue] pop)]
       (expand-users new-state login)))))

(defn expand-repos
  ([{user-graph :user-graph repo-graph :repo-graph 
     user-queue :user-queue repo-queue :repo-queue }
  login repo-name]
     (if (contains? repo-graph [login repo-name])
       {:user-graph user-graph  :user-queue user-queue
        :repo-graph repo-graph  :repo-queue repo-queue}
       (let [collabs (gitrequest/collaborators login repo-name)
             collab-logins (map :login collabs)
             new-collab-logins (remove 
                                (fn [login] (contains? user-graph login)) 
                                collab-logins)
             new-collaborators (map gitrequest/user new-collab-logins)
             new-priorities (map priority-user new-collaborators)
             new-priority-pairs (map vector new-collab-logins new-priorities)
             new-repo-graph (assoc repo-graph [login repo-name] collab-logins)
             new-user-queue (into user-queue new-priority-pairs)]
         {:user-graph user-graph      :user-queue new-user-queue
          :repo-graph new-repo-graph  :repo-queue repo-queue})))
  ([state]
   (if (empty? (state :repo-queue))
     state
     (let [[login repo-name] (first (peek (:repo-queue state)))
           new-state (update-in state [:repo-queue] pop)]
       (expand-repos new-state login repo-name)))))

