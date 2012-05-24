; Example taken from http://clojuremongodb.info/articles/getting_started.html

(ns mongocrawl.mongotest 
  (:require [monger.core :as mg])
  (:require [monger.collection :as mc])
  (:require [clojure.pprint :as pp])
  (:import [com.mongodb MongoOptions ServerAddress]))

(let [^MongoOptions opts (mg/mongo-options :threads-allowed-to-block-for-connection-multiplier 300)
      ^ServerAddress sa  (mg/server-address "arroyitos.cs.uchicago.edu" 27017)]
    (mg/connect! sa opts))

; Database is monger-test "use monger-test"
;(mg/set-db! (monger.core/get-db "monger-test"))

; Table is "documents"
;(mc/insert "documents" {:first_name "John"  :last_name "Lennon"})
;(mc/insert "documents" {:first_name "Ringo" :last_name "Starr"})

;(pp/pprint (mc/find-maps "documents" {:first_name "Ringo"}))



