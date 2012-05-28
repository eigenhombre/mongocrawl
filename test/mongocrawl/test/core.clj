(ns mongocrawl.test.core
  (:use [mongocrawl.core])
  (:require [mongocrawl.gitrequest :as gitrequest])
  (:use [clojure.data.priority-map :as priority-map])
  (:require [clojure.pprint :as pp])
  (:use expectations))

(expect 9 (priority-user (gitrequest/user "mrocklin")))
(expect 2 (priority-repo (gitrequest/specific-repo "mrocklin" "suntracker")))

(expect [[:a 1] [:a 2] [:a 3]]
        (rekey :a [1 2 3]))

(expect '([1 :a] [2 :b] [3 :c]) (map vector '(1 2 3) '(:a :b :c)))

;(expand-users "clojure")
;(expand-repos)
;(expand-users)
;(expand-repos)
;(expand-repos)
;(expand-users)
;(expand-repos)
;(expand-users)
;(expand-repos)
;(expand-users)
;(expand-repos)
;(expand-users)
;(expand-repos)
;(expand-users)
;(expand-users)
;(pp/pprint [@user-graph @repo-graph])
;(pp/pprint [@user-queue @repo-queue])

; higher priorities first
(def empty-state {:user-graph {} 
                  :repo-graph {} 
                  :user-queue (priority-map-by (comparator >))
                  :repo-queue (priority-map-by (comparator >))})
(def start-state (assoc-in empty-state [:user-queue "Clojure"] 0))
(spit "state.txt" (-> start-state expand-users expand-repos expand-users ))
