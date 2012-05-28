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

; higher priorities first
(def empty-state {:user-graph {} 
                  :repo-graph {} 
                  :user-queue (priority-map-by (comparator >))
                  :repo-queue (priority-map-by (comparator >))})
(def clojure-state (assoc-in empty-state [:user-queue "Clojure"] 0))
(def numpy-state (assoc-in empty-state [:user-queue "numpy"] 0))
(defn user-state [login]
  (assoc-in empty-state [:user-queue login] 0))
(expect numpy-state (user-state "numpy"))

(import java.io.StringWriter)
(defn hashmap-to-string [m] 
    (let [w (StringWriter.)] (pp/pprint m w)(.toString w)))

(defn write-user-network [login num-steps]
  (spit (str login ".txt") 
        (hashmap-to-string (step-n (user-state login) num-steps))))
;(spit "clojure.txt" (hashmap-to-string (step-n start-state 500)))
;(spit "numpy.txt" (hashmap-to-string (step-n start-state 500)))
