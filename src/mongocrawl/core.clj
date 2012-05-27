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
(expect 9 (priority-user (gitrequest/user "mrocklin")))
(defn priority-repo [repo]
  (:forks repo))
(expect 2 (priority-repo (gitrequest/specific-repo "mrocklin" "suntracker")))

(defn repos-of-user [login]
  (print "R") (flush)
  (map :name (gitrequest/user-repos login)))

(defn users-of-repo [login repo-name]
  (print "u") (flush)
  (map :login (gitrequest/collaborators login repo-name)))

(defn update-users [users repos]
  (let [all-users (set (apply concat (vals repos)))
        new-names (remove users all-users)
        new-repos (map repos-of-user new-names)
        new-users (zipmap new-names new-repos)]
    (merge users new-users)))

(defn rekey [login repo-names]
  (for [repo-name repo-names] [login repo-name]))
(expect [[:a 1] [:a 2] [:a 3]]
        (rekey :a [1 2 3]))

(defn users-to-userrepos [users]
  (apply concat (map #(rekey % (get users %))
       (keys users))))
(expect [[:a 1] [:a 2] [:a 3] [:b 3] [:b 4] [:b 5]]
        (users-to-userrepos {:a [1 2 3] :b [3 4 5]}   ))

(defn update-repos [users repos]
  (let [all-user-repos (users-to-userrepos users)
        new-user-repos (remove repos all-user-repos)
        new-names (map (fn [[x y]] (users-of-repo x y)) new-user-repos)
        new-repos (zipmap new-user-repos new-names)]
    (merge repos new-repos)))

(defn step
  ([users repos]
  [(update-users users repos) (update-repos users repos)])
  ([[users repos]] (step users repos)))


;;; Another whack

; higher priorities first
(def user-queue (atom (priority-map-by (comparator >)))) 
(def repo-queue (atom (priority-map-by (comparator >))))

(def user-graph (atom {}))
(def repo-graph (atom {}))

(expect '([1 :a] [2 :b] [3 :c]) (map vector '(1 2 3) '(:a :b :c)))

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

(expand-users "clojure")
(expand-repos)
(expand-users)
(expand-repos)
(expand-repos)
(expand-users)
(expand-repos)
(expand-users)
(expand-repos)
(expand-users)
(expand-repos)
(expand-users)
(expand-repos)
(expand-users)
(expand-users)
(pp/pprint [@user-graph @repo-graph])
(pp/pprint [@user-queue @repo-queue])

(defn crawl-github [start-user start-repo]
  (expand-repos start-user start-repo)
  (doseq [i (range 2)]
    (expand-users)
    (expand-repos)
    (println (format "Iteration %d: %d users, %d repos"
                     i (count @user-graph) (count @repo-graph)))))

;(crawl-github "clojure" "clojure")
;(pp/pprint @user-graph)

;(pp/pprint (gitrequest/user "mrocklin"))
;(pp/pprint (gitrequest/specific-repo "mrocklin" "suntracker"))
