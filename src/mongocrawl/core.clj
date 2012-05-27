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

(def important-repo? (fn [repo] (> (get repo :forks) 10)))
(def important-user? (fn [user] (and (> (get user :followers) 5) 
                                     (> (get user :public_repos) 20))))

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

(defn rekey [user repos]
  (for [repo repos] [user repo]))
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

;(pp/pprint (step (step {"sympy" ["sympy"]}
;                       {})))

;;; Another whack

(def users (atom {}))
(def repos (atom {}))

(defn expand-users
  ([login]
     (if-not (some #{login} @users)
       (let [repos (repos-of-user login)]
         (swap! users assoc login repos))))
  ([]
     (doseq [repo-name (keys @repos)
             login (@repos repo-name)]
       (expand-users login))))

(defn expand-repos
  ([login repo-name]
     (if-not (some #{[login repo-name]} @repos)
       (doseq [collaborator (users-of-repo login repo-name)]
         (swap! repos update-in [[login repo-name]] conj collaborator))))
  ([]
     (doseq [login (keys @users)
             repo-name (@users login)]
       (expand-repos login repo-name))))

(defn crawl-github [start-user start-repo]
  (expand-repos start-user start-repo)
  (doseq [i (range 2)]
    (expand-users)
    (expand-repos)
    (println (format "Iteration %d: %d users, %d repos"
                     i (count @users) (count @repos)))))

;(crawl-github "clojure" "clojure")
;(pp/pprint @users)

(pp/pprint (gitrequest/user "mrocklin"))
(pp/pprint (gitrequest/specific-repo "mrocklin" "suntracker"))
(defn priority-user [user]
  (min (:followers user) (:public_repos user)))
(expect 9 (priority-user (gitrequest/user "mrocklin")))
(defn priority-repo [repo]
  (:forks repo))
(expect 2 (priority-repo (gitrequest/specific-repo "mrocklin" "suntracker")))



