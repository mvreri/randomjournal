(ns journal.routes.home
    (:require [compojure.core :refer :all]
      [taoensso.timbre :as timbre]
      [journal.layout :as layout]
      [journal.config :as config]
      [journal.encrypt :as encrypt]
      [journal.database :as db]
      [clojure.data.json :as json]
      [noir.response :refer [redirect]]
      [clojure.data.json :as json]
      [clojure.walk :as walk]
      [noir.util.crypt :as crypt]
      [noir.session :as session]
      [noir.response :as response]
      [clj-http.client :as client]
      [noir.util.route :refer [restricted def-restricted-routes]]
      [taoensso.timbre :refer [trace debug info warn error fatal]]
      [cronj.core :refer [cronj]]
      [noir.io :as io]
      [noir.util.crypt :as crypt]
      [clj-time.core :as t]
      [clj-time.local :as l]
      [clj-time.format :as f]
      [clj-time.coerce :as tc]
      [clj-time.jdbc :as jd]
      [clojure.string :as string]
      [cheshire.core :refer :all]
      [crypto.password.scrypt :as password]
      [ring.util.http-response :refer :all]           ;ring http response

      )
    (:use (clojure.java [io :as jio]))
    (:use [slingshot.slingshot :only [throw+ try+]])
    (import (java.io File)
            (java.net URL
                      URLConnection
                      HttpURLConnection
                      UnknownHostException)
            (java.net URLEncoder)
            [org.apache.pdfbox.pdmodel PDDocument]
            [org.apache.pdfbox.util PDFTextStripper])
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
       :session (:session (session/get :sssn_tp))
       :today (get-today)
       :year (get-year)
       :now (get-now)
       }
      )

;pages
(defn journals-page [req]
      (let [
            flashmsg (session/flash-get :message)
            link (:link (:link (session/get :sssn_tp)))
            final-page-params (assoc (page-load (str "Journaling ")  (str "An overview of your journaling account. Have fun!")) :pagemsg flashmsg)
            final-page-params (assoc final-page-params :link link)
            ]
           (layout/render "journaling.html" final-page-params)
           )
      )

(defn journal-page [req]
      (let [
            flashmsg (session/flash-get :message)
            link (:link (:link (session/get :sssn_tp)))
            final-page-params (assoc (page-load (str "Journaling ")  (str "An overview of your journaling account. Have fun!")) :pagemsg flashmsg)
            final-page-params (assoc final-page-params :link link)
            ]
           (layout/render "journaling.html" final-page-params)
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
            final-page-params (assoc (page-load (str "Random Journaling")  (str "Random Journaling Page")) :pagemsg flashmsg)
            ]
           (layout/render "index.html" final-page-params)
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
      (let [registercreds (assoc registercreds :application config/application)]
           (let [createprofile (:body (client/post (str config/ip-port-ext "/api/v1/account/create") {
                                                                                                      :body (json/write-str registercreds)
                                                                                                      :body-encoding "UTF-8"
                                                                                                      :content-type  :json
                                                                                                      }
                                                   )
                                 )
                 ]
                createprofile
                )

           )
      )

(defn registration-page [req]
      (session/put! :sssn_pg (get-in req [:uri]))
      (timbre/info "Accessed ++++++++ " (session/get :sssn_pg))
      (let [
            flashmsg (session/flash-get :message)
            final-page-params (assoc (page-load (str "Random Journaling Registration")  (str "Random Journaling Registration" )) :pagemsg flashmsg)
            ]
           (layout/render "register.html" final-page-params)
           )
      )

(defn set-user [session]
      (session/put! :sssn_in true)
      )

(defn login [logincreds]
      (timbre/info "Starting User Login Process on login fn..." )
      (let [username (if (not= (:username logincreds) nil) (string/trim (:username logincreds)) (:username logincreds))
            password (:password logincreds)
            url (str config/ip-port-ext "/api/v1/account/access")]
           (if (not= username nil)
             (let [;_ (println (found url))
                   login (:body (client/post url
                                             {
                                              :body          (json/write-str
                                                               {
                                                                :apikey (crypt/encrypt "journal.key")
                                                                :username username
                                                                :password password
                                                                }
                                                               )
                                              :body-encoding "UTF-8"
                                              :content-type  :json
                                              }
                                             )
                           )
                   ]
                  (if (= (contains? (walk/keywordize-keys (json/read-str login)) :errors) true)
                    login
                    (do
                      (set-user (:session (walk/keywordize-keys (get-in (json/read-str login) ["data"]))))
                      (json/write-str {:data (dissoc (walk/keywordize-keys (get-in (json/read-str login) ["data"])) :session)})
                      )
                    )
                  )
             (json/write-str {:errors {
                                       :status  403
                                       :title   (str "Incorrect Username Format")
                                       :detail  (str "Incorrect Username Format")
                                       :message (str "Incorrect Email/Phone Number Format")
                                       }
                              })

             )
           )

      )

(defn login-page [req]
      (session/put! :sssn_pg (get-in req [:uri]))
      (timbre/info "Accessed ++++++++ " (session/get :sssn_pg))
      ;(timbre/info "Accessed ++++++++ " (session/get :sssn_pg))
      ;(timbre/info "Login Status? " (true? (session/get :sssn_in)))
      ;(timbre/info "Login Status " (session/get :sssn_in))
      (let [
            flashmsg (session/flash-get :message)
            final-page-params (assoc (page-load (str "Sign In")  (str "Welcome Back")) :pagemsg flashmsg)
            ]
           (layout/render "login.html" final-page-params)
           )
      )

(defn logout []
      (timbre/info "Logging Out")
      (session/clear!)
      ;(response/redirect "/login")
      )

(defn signout-page []
      (let [
            flashmsg (session/flash-put! :message "Successfully Logged Out")
            final-page-params (assoc (page-load (str "Logging Out")  (str "Logging Out from your account")) :pagemsg flashmsg)
            _ (logout)
            ]
           (layout/render "logout.html" final-page-params)
           )
      )

(defn logout-page []
      (let [
            flashmsg (session/flash-put! :message "Successfully Logged Out")
            final-page-params (assoc (page-load (str "Signing Out")  (str "Signing out from your account")) :pagemsg flashmsg)
            _ (logout)
            ]
           (layout/render "logout.html" final-page-params)
           )
      )



(def-restricted-routes home-routes
                       (GET "/journall" req (journal-page req))
                       ;(POST "/journal" [jrn] (journal jrn))
                       ;(POST "/journal/check" [jrn] (journal-check jrn))
                       (GET "/journals" req (journals-page req))
                       )
(defroutes open-routes
           ;main site pages
           (GET "/" req (home-page req))
           (GET "/journal" req (journal-page req))
           (POST "/journal" [jrn] (journal jrn))
           (POST "/journal/check" [jrnd] (journal-check jrnd))
           ;user account pages
           (GET "/register" req (registration-page req))
           (POST "/register" [registercreds] (register registercreds))
           (POST "/register/check/email" [email] (register-check-email email))
           (GET "/login" req (login-page req))
           (POST "/login" [logincreds] (login logincreds))
           (GET "/sign-out" [] (signout-page ))
           (GET "/logout" [] (logout-page ))
           (POST "/logout" [] (logout ))
           )
