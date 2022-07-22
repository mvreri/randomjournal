(ns journal.database
    (:require
      [compojure.core :refer :all]
      [taoensso.timbre :as timbre]
      [journal.layout :as layout]
      [journal.config :as config]
      [journal.encrypt :as encrypt]
      [taoensso.timbre :as timbre]
      [ring.middleware.json :as middleware]
      [clojure.data.json :as json]
      [korma.db :refer :all]
      [journal.config :as config]
      [journal.encrypt :as en]
      [clj-http.client :as httpclient]
      [clj-time.coerce :as tc]
      [clj-http.client :as client]
      [clojure.string :as string]
      [clojure.walk :as walk]
      [korma.core :refer :all]
      [clj-time.core :as t]
      [clj-time.format :as f]
      [clj-time.coerce :as tc]
      [clj-time.local :as l]
      [clj-time.jdbc :as jd]
      [ring.util.codec :as codec]
      [crypto.password.scrypt :as password]
      [crypto.password.pbkdf2 :as passwordpbkdf2]
      [clojure.java.io :as io]
      [postal.core :refer [send-message]]
      [com.climate.claypoole :as cp]

      )
    (:use (clojure.java [io :as jio]))
    (:import java.util.concurrent.Executors)
    (:import java.util.concurrent.TimeUnit)
    (import java.util.Base64)
    (:import (org.jsoup Jsoup)
      [javax.crypto Cipher]
      [javax.crypto.spec SecretKeySpec]
      [java.security MessageDigest Key]
      [java.util Base64 Base64$Encoder Base64$Decoder]
      ;(org.apache.commons.codec.binary Base64)
      (java.nio.charset StandardCharsets)
      (javax.security.cert Certificate))
    )

(config/initialize-config)
;aux functions
(def numerickey "1234567890")
(defn get-random-numeric-key [length]
      (loop [acc []]
            (if (= (count acc) length) (apply str acc)
                                       (recur (conj acc (rand-nth numerickey))))))

(def alphabetic "ABCDEFGHIJKLMNOPQRSTUVWXYZ")
(defn get-random-alphabetic-id [length]
      (loop [acc []]
            (if (= (count acc) length) (apply str acc)
                                       (recur (conj acc (rand-nth alphabetic))))))

(def alphanumeric "ABCDEFGHJKLMNPQRSTUVWXYZ1234567890")
(defn get-random-id [length]
      (loop [acc []]
            (if (= (count acc) length) (apply str acc)
                                       (recur (conj acc (rand-nth alphanumeric))))))

(def alphanumerickey "abcdefghijklm1234567890nopqrstuvwxyz")
(defn get-random-key [length]
      (loop [acc []]
            (if (= (count acc) length) (apply str acc)
                                       (recur (conj acc (rand-nth alphanumerickey))))))



(defn get-current-timestamp []
      (str (f/unparse (f/formatter-local "yyyy-MM-dd HH:mm:ss") (l/local-now)))
      )


(declare log-every-x-secs)
(def pool (cp/threadpool 2))

(defn set-interval [callback ms]
      (future (while true (do (Thread/sleep ms) (callback)))))

(def job (set-interval #(log-every-x-secs) 1000))
;(def job (set-interval #(log-every-x-secs) 43200))          ;every 12 hrs

(future-cancel job)



(def db-connection-journal
  {:classname "org.postgresql.Driver"
   :subprotocol config/db-subprotocol
   :user config/db-user
   :password config/db-pass
   :subname config/db-subname
   })


(def pool (cp/threadpool 2))

(defn set-interval [callback ms]
      (future (while true (do (Thread/sleep ms) (callback)))))

;(def job (set-interval #(log-every-x-secs) 1000))
(def job (set-interval #(log-every-x-secs) 43200))          ;every 12 hrs
;(def job (set-interval #(log-every-x-secs) 300000))

(future-cancel job)


(defn log-every-x-secs []
      (timbre/info "logging again")
      )

(defn cancel-job []
      (future-cancel job)
      )


(defn journal-get-by-date [date]
      (with-db db-connection-journal (exec-raw (format "SELECT details::text details
                                                   FROM tbl_rjournal
                                                   WHERE DATE(created)= CURDATE()
                                                   AND '%s' = CURDATE();"
                                                       date) :results)
               )
      )

(defn journal-get-by-refno [urlref]
      (with-db db-connection-journal (exec-raw (format "SELECT details::text details
                                                   FROM tbl_rjournal
                                                   WHERE refno='%s';"
                                                      urlref) :results)
               )
      )

(defn journal-create [body]
      (let [
            jrnlsuffix (get-random-id 5)
            refno (str "JRNL-" jrnlsuffix)
            bodyalt (assoc body :refno refno)
            bodyalt (assoc bodyalt :created (get-current-timestamp))
            ]
           (with-db db-connection-journal (exec-raw (format "INSERT INTO tbl_rjournal(refno, details)
                                                              VALUES ('%s','%s');"
                                                           refno
                                                           (clojure.string/replace (json/write-str bodyalt) #"'" "''")
                                                           )))



           (journal-get-by-refno refno)
           )
      )

(defn journal-send [body]
      (let [
            body (assoc body :created (get-current-timestamp))
            ;pick a random user to send journals to
            juser (first (with-db db-connection-journal (exec-raw (format "SELECT refno, details->>'email'::text email
                                                                      FROM tbl_rjournal_users
                                                                      ORDER BY random() limit 1;"):results
                                                                 )
                                  )
                         )
            somecontent (with-db db-connection-journal (exec-raw (format "SELECT details FROM tbl_rjournal
                                                                          WHERE DATE(created) = CURDATE();"
                                                                         (:refno juser)
                                                                         )
                                                                 )
                                 )
            ]
           (println somecontent)

           ;;update the journal table with the id of the user we picked for today
           #_(with-db db-connection-journal (exec-raw (format "UPDATE tbl_rjournal
                                                     SET sendout = '%s'
                                                     WHERE DATE(created) = CURDATE();"
                                                           (:refno juser)
                                                           )
                                                   )
                    )
           ;send the journals to the user via email (:email juser)
           #_(send-message {:host "smtp.zoho.com"
                            :ssl true
                            :user (str config/send-email-official)
                            :pass (str config/send-email-official-pass)
                            :port (Integer/parseInt config/send-email-official-port)
                            }
                           {
                            :from (str config/send-email-official-name  " <" config/send-email-official">")
                            :to (:email body)
                            :subject "Random Journal"
                            :body [{:type "text/html"
                                    :content somecontent }
                                   ]
                            }
                           )



           201
           )
      )

(defn journal-user-get-by-email [email]
      (first (with-db db-connection-journal (exec-raw (format "SELECT details::text details
                                                            FROM tbl_rjournal_users
                                                            WHERE details->>'email'::text='%s';"
                                                             email) :results)
                      )
             )
      )

(defn journal-user-get-by-refno [userrefno]
      (with-db db-connection-journal (exec-raw (format "SELECT details::text details
                                                            FROM tbl_rjournal_users
                                                            WHERE refno='%s';"
                                                      userrefno) :results)
               )
      )

(defn journal-users-create [body]
      (let [
            refno (get-random-id 5)
            bodyalt (assoc body :refno refno)
            bodyalt (assoc bodyalt :created (get-current-timestamp))
            ]
           (with-db db-connection-journal (exec-raw (format "INSERT INTO tbl_rjournal_users(refno, details)
                                                              VALUES ('%s','%s');"
                                                           refno
                                                           (clojure.string/replace (json/write-str bodyalt) #"'" "''")
                                                           )))



           (journal-user-get-by-refno refno)
           )
      )
