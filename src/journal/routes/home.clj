(ns journal.routes.home
  (:require [compojure.core :refer :all]
            [taoensso.timbre :as timbre]
            [journal.layout :as layout]
            [journal.config :as config]
            [journal.database :as db]
            [clojure.data.json :as json]
            [noir.response :refer [redirect]]
            [clojure.data.json :as json]
            [clojure.walk :as walk]
            [noir.session :as session]
            [noir.response :as response]
            [clj-http.client :as client]
            [noir.util.route :refer [restricted def-restricted-routes]]
            [taoensso.timbre :refer [trace debug info warn error fatal]]
            [cronj.core :refer [cronj]]
            [clj-time.local :as l]
            [clj-time.format :as f]
            [clojure.string :as string]
            [cheshire.core :refer :all]
            [ring.util.http-response :refer :all]           ;ring http response
            [chime.core :as chime]

            )
  (:use (clojure.java [io :as jio]))
  (:use [slingshot.slingshot :only [throw+ try+]])
  (import [java.time Instant]
          [java.time LocalTime ZonedDateTime ZoneId Period])
  )



(config/initialize-config)

;strip characters froma string
(defn strip [coll chars]
  (apply str (remove #((set chars) %) coll)))
;(strip "She was a soul stripper. She took my heart!" "aei")
;; => "Sh ws  soul strppr. Sh took my hrt!"

(defn get-port [req]
  (if (= (get-in req [:server-port]) nil) "" (str ":" (get-in req [:server-port])))
  )

(defn get-prefix [req port]
  (str (name (get-in req [:scheme])) "://" (get-in req [:server-name]) port)
  )

(defn get-year []
  (clj-time.format/unparse (clj-time.format/formatter-local "yyyy") (clj-time.local/local-now))
  )

(defn get-today []
  (clj-time.format/unparse (clj-time.format/formatter-local "dd-MM-yyyy") (clj-time.local/local-now))
  )

(defn get-now []
  (clj-time.format/unparse (clj-time.format/formatter-local "yyyy-MM-dd hh24:mm:ss") (clj-time.local/local-now))
  )

(defn create-url-from-string [urlable-string]
  (string/lower-case (str (.replaceAll (.replaceAll (get (clojure.string/split urlable-string #"\.") 0) "[^0-9A-Za-z ]" "") " " "-") "-" (db/get-random-id 2)))
  )

(defn get-current-timestamp []
  (str (f/unparse (f/formatter-local "yyyy-MM-dd HH:mm:ss") (l/local-now)))
  )


(defn numeric? [s]
  (if-let [s (seq s)]
    (let [s (if (= (first s) \-) (next s) s)
          s (drop-while #(Character/isDigit %) s)
          s (if (= (first s) \.) (next s) s)
          s (drop-while #(Character/isDigit %) s)]
      (empty? s))))

(defn validate-email
  [email]
  (let [pattern #"[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?"]
    (and (string? email) (re-matches pattern email))))


(defn page-load [page-name page-description]
  {
   :page-title (str page-name)
   :page-description  (str page-description)
   :today (get-today)
   :year (get-year)
   :now (get-now)
   }
  )

;pages
(defn journals-page [req]
  (let [
        flashmsg (session/flash-get :message)
        session (session/get :sssn)
        final-page-params (assoc (page-load (str "Random Journaling | Journals ")  (str "An overview of your journals")) :pagemsg flashmsg)
        final-page-params (assoc final-page-params :session session)
        ]
    (layout/render "journals.html" final-page-params)
    )
  )
(defn journals [jrnusr]
  (let [jrnlsbyuser (db/journal-get-by-user jrnusr)]
    jrnlsbyuser
    )
  )

(defn journals-random [jrnusr]
  (let [jrnlsrand (db/journal-get-by-user jrnusr)]
    jrnlsrand
    )
  )

(defn journal-page [req]
  (let [
        flashmsg (session/flash-get :message)
        session (session/get :sssn)
        final-page-params (assoc (page-load (str "Random Journaling | Journal ")  (str "An overview of your journaling account. Have fun!")) :pagemsg flashmsg)
        final-page-params (assoc final-page-params :session session)
        ]

    ;(layout/render "journaling.html" final-page-params)
    (layout/render "journal.html" final-page-params)
    )
  )

(defn journal-check [jrnd]
  ;user, date, journalentry
  (let [jcheck (db/journal-get-by-date jrnd)]
    jcheck
    )
  )

(defn journal [jrn]
  ;user, date, journalentry
  (let [createprofile (db/journal-create jrn)
        ]
    createprofile
    )
  )

(defn home-page [req]
  (session/put! :sssn_pg (get-in req [:uri]))
  (let [
        flashmsg (session/flash-get :message)
        final-page-params (assoc (page-load (str "Random Journaling | Home")  (str "Random Journaling Home")) :pagemsg flashmsg)
        ]
    (layout/render "login.html" final-page-params)
    )
  )

(defn register-check-email [email]
  (let [userbyemail (:body (client/get (str config/ip-port-ext "/api/v1/account/view/email/" email){
                                                                                                    :body-encoding "UTF-8"
                                                                                                    :content-type  :json
                                                                                                    }
                                       )
                      )
        ]
    userbyemail
    )
  )

(defn register [registercreds]
  ;email, password, name
  (let [createprofile (db/journal-users-create registercreds)
        ]
    createprofile
    )
  )

(defn registration-page [req]
  (session/put! :sssn_pg (get-in req [:uri]))
  (let [
        flashmsg (session/flash-get :message)
        final-page-params (assoc (page-load (str "Random Journaling | Registration")  (str "Random Journaling Registration" )) :pagemsg flashmsg)
        ]
    (layout/render "register.html" final-page-params)
    )
  )

(defn set-user [usr]
  (session/put! :sssn_in true)
  (session/put! :sssn usr)
  )

(defn login [logincreds]
  (timbre/info "Starting User Login Process on login fn..." )
  (let [login (db/journal-users-login logincreds)]
    (if (contains? (walk/keywordize-keys (json/read-str login)) :data)
      (set-user (:email logincreds))
      )
    login
    )
  )

(defn login-page [req]
  (session/put! :sssn_pg (get-in req [:uri]))
  (let [
        flashmsg (session/flash-get :message)
        final-page-params (assoc (page-load (str "Random Journaling | Sign In")  (str "Welcome Back")) :pagemsg flashmsg)
        ]
    (layout/render "login.html" final-page-params)
    )
  )

(defn logout []
  (timbre/info "Logging Out")
  (session/clear!)
  (response/redirect "/login")
  )

(defn signout-page []
  (let [
        flashmsg (session/flash-put! :message "Successfully Logged Out")
        final-page-params (assoc (page-load (str "Random Journaling | Logging Out")  (str "Logging Out from your account")) :pagemsg flashmsg)
        _ (logout)
        ]
    (layout/render "logout.html" final-page-params)
    )
  )

(defn logout-page []
  (let [
        flashmsg (session/flash-put! :message "Successfully Logged Out")
        final-page-params (assoc (page-load (str "Random Journaling | Signing Out")  (str "Signing out from your account")) :pagemsg flashmsg)
        _ (logout)
        ]
    (layout/render "logout.html" final-page-params)
    )
  )



(def-restricted-routes home-routes
                       (GET "/journals" req (journals-page req))
                       (POST "/journals" [jrnusr] (journals jrnusr))
                       (GET "/journal" req (journal-page req))
                       (POST "/journal" [jrn] (journal jrn))
                       (POST "/journal/check" [jrnd] (journal-check jrnd))
                       (POST "/journals/random" [jrnusr] (journals-random jrnusr))
                       (GET "/sign-out" [] (signout-page ))
                       (GET "/logout" [] (logout-page ))
                       (POST "/logout" [] (logout ))
                       )
(defroutes open-routes
           ;main site pages
           (GET "/" req (home-page req))
           ;user account pages
           (GET "/register" req (registration-page req))
           (POST "/register" [registercreds] (register registercreds))
           (POST "/register/check/email" [email] (register-check-email email))
           (GET "/login" req (login-page req))
           (POST "/login" [logincreds] (login logincreds))

           )
