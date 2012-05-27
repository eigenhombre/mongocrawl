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

; (user "mrocklin) 
; (user "mrocklin"
(defn user [login]
  (if (seq (mc/find-maps "users" {:login login})) ; if not empty
      (first (mc/find-maps "users" {:login login}))
      (let [result (users/user login)]
          (mc/insert "users" result)
          result)))

(defn specific-repo [login repo-name]
  (let [db-results (mc/find-maps "repos" {"owner.login" login :name repo-name})]
    (if (seq db-results)
       (first db-results)
       (let [api-result (repos/specific-repo login repo-name)]
           (mc/insert "repos" api-result)
           api-result))))

(defn user-repos [login]
  (let [db-result (mc/find-maps "repos" {"owner.login" login})]
       (if (seq db-result)
           db-result
           (let [api-results (repos/user-repos login)]
           (mc/insert-batch "repos" api-results)
           api-results))))

(defn collaborators [login repo-name]
  (let [db-result 
        (mc/find-maps "collaborators" {:owner-login login :repo repo-name})]
    (if (seq db-result)
      db-result
      (let [api-results (repos/collaborators login repo-name)
            api-results2 (map #(merge {:owner-login login :repo repo-name} %) 
                              api-results)]
        (mc/insert-batch "collaborators" api-results2)
        api-results2))))

; (drop "users")
(defn drop-table [table]
  (mc/remove table))

(setup)
; (mc/remove "users")
; (mc/remove "repos")

(expect "http://matthewrocklin.com" (get (user "mrocklin") :blog))
(expect 1 (count (user-repos "languagejam")))
(expect "test" (:name (first (user-repos "languagejam"))))
(expect 1 (count (mc/find-maps "users" {:login "mrocklin"}))) ;occur once in db
(expect 1 (count (mc/find-maps "repos" 
                               {:name "test" "owner.login" "languagejam"})))
(expect #{"mrocklin" "eigenhombre"} (set (map :login (collaborators "eigenhombre" "mongocrawl"))))
(expect "git://github.com/mrocklin/matrix-algebra.git" 
        (:git_url (specific-repo "mrocklin" "matrix-algebra")))
(expect 1 (count (mc/find-maps "repos" {:name "matrix-algebra"})))
