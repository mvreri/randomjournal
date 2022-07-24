(ns journal.database
  (:require
    [compojure.core :refer :all]
    [taoensso.timbre :as timbre]
    [journal.config :as config]
    [taoensso.timbre :as timbre]
    [clojure.data.json :as json]
    [korma.db :refer :all]
    [journal.config :as config]
    [clj-time.coerce :as tc]
    [clojure.string :as string]
    [clojure.walk :as walk]
    [korma.core :refer :all]
    [clj-time.core :as t]
    [clj-time.format :as f]
    [clj-time.coerce :as tc]
    [clj-time.local :as l]
    [clj-time.jdbc :as jd]
    [crypto.password.scrypt :as password]
    [postal.core :refer [send-message]]
    [com.climate.claypoole :as cp]
    )
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


(declare log-every-x-secs journal-send)
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

(def job (set-interval #(log-every-x-secs) 60000))
;(def job (set-interval #(log-every-x-secs) 43200))          ;every 12 hrs
;(def job (set-interval #(log-every-x-secs) 300000))

(future-cancel job)

;check the time to send the email every minute
(defn log-every-x-secs []
  (timbre/info "logging again")
  ;(timbre/info (.format (java.text.SimpleDateFormat. "MM/dd/yyyy") (new java.util.Date)))
  ;(timbre/info (.format (java.text.SimpleDateFormat. "HH:mm") (new java.util.Date)))
  ;(timbre/info "__ " (get-current-timestamp))
  ;(timbre/info "__ " (str (f/unparse (f/formatter-local "yyyy-MM-dd") (l/local-now)) " 12:00"))
  ;(timbre/info "__ " (= (str (f/unparse (f/formatter-local "HH:mm") (l/local-now))) "01:41") )
  ;(timbre/info "__ " (= (str (f/unparse (f/formatter-local "yyyy-MM-dd HH:mm") (l/local-now))) (str (f/unparse (f/formatter-local "yyyy-MM-dd") (l/local-now)) " 02:11")) )
  ;

  (if (= (str (f/unparse (f/formatter-local "HH:mm") (l/local-now))) "12:00")
    (journal-send)
    )

  )

(defn cancel-job []
  (future-cancel job)
  )


(defn journal-get-by-date [body]
  (let [j (with-db db-connection-journal (exec-raw (format "SELECT dttt::text details
                                                            FROM tbl_rjournal, jsonb_array_elements(details) with ordinality arr(dttt, index)
                                                            WHERE DATE(created) = current_date
                                                            AND dttt->>'user' = '%s';"
                                                           (:user body)
                                                           ):results
                                                   )
                   )]
    (with-out-str (json/pprint {:data {
                                       :status 200
                                       :title (str "Journal" )
                                       :detail  j
                                       }
                                })
                  )
    )
  )

(defn journal-get-by-refno [urlref]
  (:details (first (with-db db-connection-journal (exec-raw (format "SELECT details::text details
                                                                         FROM tbl_rjournal
                                                                         WHERE refno='%s';"
                                                                    urlref) :results)
                            )
                   )
    )
  )

(defn journal-get-by-user [user]
  (let [j (with-db db-connection-journal (exec-raw (format "SELECT dttt::text details
                                                            FROM tbl_rjournal, jsonb_array_elements(details) with ordinality arr(dttt, index)
                                                            WHERE dttt->>'user' = '%s';"
                                                           user
                                                           ):results
                                                   )
                   )]

    (with-out-str (json/pprint {:data {
                                       :status 200
                                       :title (str "Journals" )
                                       :detail  j
                                       }
                                })
                  )
    )

  )

(defn journal-create [body]
  (let [
        jrnlsuffix (get-random-id 5)
        refno (str "JRNL-" jrnlsuffix)
        bodyalt (assoc body :refno refno)
        bodyalt (assoc bodyalt :created (get-current-timestamp))
        ]

    ;check whether theres a date for today, if theres none, create one entry
    (let [tdj (:refcnt (first (with-db db-connection-journal (exec-raw (format "SELECT count(refno) refcnt
                                                                          FROM tbl_rjournal
                                                                          WHERE DATE(created) = current_date;"
                                                                               ) :results)
                                       )))]
      (if (> tdj 0)
        (with-db db-connection-journal (exec-raw (format "UPDATE tbl_rjournal
                                                                  SET details = COALESCE(details, '[]'::jsonb) || '%s' ::jsonb
                                                                  WHERE DATE(created) = current_date;"
                                                         (clojure.string/replace (json/write-str bodyalt) #"'" "''")
                                                         )
                                                 )
                 )
        (do
          (with-db db-connection-journal (exec-raw (format "INSERT INTO tbl_rjournal(refno)
                                                                  VALUES ('%s');"
                                                           refno
                                                           )))
          (with-db db-connection-journal (exec-raw (format "UPDATE tbl_rjournal
                                                                  SET details = COALESCE(details, '[]'::jsonb) || '%s' ::jsonb
                                                                  WHERE refno = '%s';"
                                                           (clojure.string/replace (json/write-str bodyalt) #"'" "''")
                                                           refno
                                                           )
                                                   )
                   )
          )
        )
      )

    (with-out-str (json/pprint {:data {
                                       :status 201
                                       :title (str "Journaling" )
                                       :detail  (journal-get-by-refno refno)
                                       }
                                })
                  )
    )
  )

(defn join-msgs [s]
  (->> s
       (map-indexed #(str (inc %1) ". " %2))
       (interpose \newline)
       (apply str)))

(defn journal-send []
  (let [
        ;pick a random user to send journals to
        juser (first (with-db db-connection-journal (exec-raw (format "SELECT details->>'email'::text email
                                                                      FROM tbl_rjournal_users
                                                                      ORDER BY random() limit 1;"):results
                                                              )
                              )
                     )
        somecontent (with-db db-connection-journal (exec-raw (format "SELECT dttt->>'message'::text msg
                                                                          FROM tbl_rjournal, jsonb_array_elements(details) with ordinality arr(dttt, index)
                                                                          WHERE DATE(created) = current_date;"
                                                                     ) :results
                                                             )
                             )
        ]

    (if (not= somecontent nil)
      (do
        ;;update the journal table with the id of the user we picked for today
        (with-db db-connection-journal (exec-raw (format "UPDATE tbl_rjournal
                                                                 SET sendout = '%s'
                                                                 WHERE DATE(created) = current_date;"
                                                         (:email juser)
                                                         )
                                                 )
                 )

        ;send the journals to the user via email (:email juser)
        (send-message {:host "smtp.gmail.com"
                       :tls true
                       :user (str "year.ninety@gmail.com")
                       :pass (str "password")
                       :port 587
                       }
                      {
                       :from (str "Journaling"  " <year.ninety@gmail.com>")
                       :to (:email juser)
                       :subject "Random Journal"
                       :body [{:type "text/html"
                               :content (join-msgs (vec (map :msg (vec somecontent)))) }
                              ]
                       }
                      )
        (with-out-str (json/pprint {:data {
                                           :status 201
                                           :title (str "Journal Sent" )
                                           :detail  (str "OK" )
                                           }
                                    })
                      )

        )
      (with-out-str (json/pprint {:errors {
                                           :status 404
                                           :title (str "Journal Sent" )
                                           :detail  (str "No journal sent" )
                                           }
                                  })
                    )

      )






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
  (:details (first (with-db db-connection-journal (exec-raw (format "SELECT details::text details
                                                            FROM tbl_rjournal_users
                                                            WHERE refno='%s';"
                                                                    userrefno) :results)
                            )
                   )
    )
  )

(defn journal-users-create [body]
  (let [uemail (journal-user-get-by-email (:email body))]
    (if (= uemail nil)
      (let [
            refno (get-random-id 5)
            bodyalt (assoc body :refno refno)
            bodyalt (assoc bodyalt :active true)
            bodyalt (assoc bodyalt :password (password/encrypt (:password bodyalt)))
            bodyalt (assoc bodyalt :created (get-current-timestamp))

            ]

        (with-db db-connection-journal (exec-raw (format "INSERT INTO tbl_rjournal_users(refno, details)
                                                              VALUES ('%s','%s');"
                                                         refno
                                                         (clojure.string/replace (json/write-str bodyalt) #"'" "''")
                                                         )))
        (let [
              udetails (journal-user-get-by-refno refno)
              ud (walk/keywordize-keys (json/read-str udetails))
              _ (dissoc ud :password)
              ]
          (with-out-str (json/pprint {:data {
                                             :status 201
                                             :title (str "Registration" )
                                             :detail  (str "OK" )
                                             }
                                      })
                        )
          )
        )
      (with-out-str (json/pprint {:errors {
                                           :status 403
                                           :title (str "Registration" )
                                           :detail  (str "Email already in use" )
                                           }
                                  })
                    )

      )
    )

  )

(defn journal-users-login [body]
  (let [
        uemail (journal-user-get-by-email (:email body))
        ]

    (if (= uemail nil)
      (with-out-str (json/pprint {:errors {
                                           :status 404
                                           :title (str "Login" )
                                           :detail  (str "No Account" )
                                           }
                                  })
                    )
      (do
        (if (password/check  (:password body) (:password (walk/keywordize-keys (json/read-str (:details uemail)))))
          (with-out-str (json/pprint {:data {
                                             :status 200
                                             :title (str "Login" )
                                             :detail  (str "OK" )
                                             }
                                      })
                        )
          (with-out-str (json/pprint {:errors {
                                               :status 403
                                               :title (str "Login" )
                                               :detail  (str "Not Allowed" )
                                               }
                                      })
                        )
          )
        )
      )
    )
  )
