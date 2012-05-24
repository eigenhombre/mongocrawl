(ns mongocrawl.gitrequest
  (:use [expectations])
  (:require [clojure.pprint :as pp])
  (:require [tentacles.users :as users])
  (:require [tentacles.repos :as repos])
  (:require [monger.core :as mg])
  (:require [monger.collection :as mc])
  (:require [clojure.pprint :as pp])
  (:import [com.mongodb MongoOptions ServerAddress]))

(defn connect 
  " Connects to a mongo database "
  ([servername port]
    (let [^MongoOptions opts (mg/mongo-options :threads-allowed-to-block-for-connection-multiplier 300)
      ^ServerAddress sa  (mg/server-address servername port)]
    (mg/connect! sa opts)))
  ([] (connect "arroyitos.cs.uchicago.edu" 27017)))

(def db-name "github")
(defn set-db! [name]
  (mg/set-db! (monger.core/get-db name)))

(defn setup 
  ([server port name]  (connect server port)  (set-db! name))
  ([] (connect) (set-db! db-name)))

(defn repos-of-user [u]
  (print "R") (flush)
  (map :name (remove :fork (repos/user-repos u))))

(defn users-of-repo [u r]
  (print "u") (flush)
  (map :login (repos/collaborators u r)))

; (user "mrocklin) 
; (user "mrocklin"
(defn user [login]
  (if (seq (mc/find-maps "users" {:login login})) ; if not empty
      (mc/find-maps "users" {:login login})
      (let [result (users/user login)]
          (mc/insert "users" result)
          result)))

(defn repos-of-user [login]
  (let [db-result (mc/find-maps "repos" {"owner.login" login})]
       (if (seq db-result)
           db-result
           (let [api-results (repos/user-repos login)]
           (mc/insert-batch "repos" api-results)
           api-results))))

; (drop "users")
(defn drop-table [table]
  (mc/remove table))

(setup)

(expect "http://matthewrocklin.com" (get (first (user "mrocklin")) :blog))
(expect 1 (count (repos-of-user "languagejam")))
(expect "test" (:name (first (repos-of-user "languagejam"))))
(expect 1 (count (mc/find-maps "users" {:login "mrocklin"}))) ;occur once in db
(expect 1 (count (mc/find-maps "repos" 
                               {:name "test" "owner.login" "languagejam"})))

