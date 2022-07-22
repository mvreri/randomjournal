(ns journal.config
    ^{:author "Mureri "
      :doc    "Loads the configuration files for the journal"
      :added  "1.0"
      }
    (:require [clojure.tools.logging :as log])
    (:import [java.util Properties]
      [java.io FileInputStream]
      )
    (:use [clojure.walk])
    (:use korma.core)
    (:use korma.db))

(def app-configs (atom {}))

(defn- config-file []
       (let [result (let [result (System/getenv "journal")]
                         (when result (java.io.File. result)))]
            (if (and result (.isFile result))
              result
              (do (log/fatal (format "serverConfig(%s) = nil" result))
                  (throw (Exception. (format "Server configuration file (%s) not found." result)))))))

(defn load-config [config]
      (let [properties (Properties.)
            fis (FileInputStream. config)]
           ; populate the properties hashmap with values from the output stream
           (.. properties (load fis))
           (keywordize-keys (into {} properties))))

(defn config-value [name & args]
      (let [value (@app-configs name)]
           (when value
                 (let [args (when args (apply assoc {} args))
                       {type :type} args
                       args (dissoc args :type)
                       value (if (vector? value)
                               (loop [x (first value)
                                      xs (next value)]
                                     (let [properties (dissoc x :value)]
                                          (if (or (and (empty? args)
                                                       (empty? properties))
                                                  (and (not (empty? args))
                                                       (every? (fn [[k v]]
                                                                   (= (properties k) v))
                                                               args)))
                                            (x :value)
                                            (when xs
                                                  (recur (first xs) (next xs))))))
                               value)]
                      (when value
                            (let [value #^String value]
                                 (cond (or (nil? type) (= type :string)) value
                                       ;; ---
                                       (= type :int) (Integer/valueOf value)
                                       (= type :long) (Long/valueOf value)
                                       (= type :bool) (contains? #{"yes" "true" "y" "t" "1"}
                                                                 (.toLowerCase value))
                                       (= type :path) (java.io.File. value)
                                       (= type :url) (java.net.URL. value)
                                       )))))))

(defn initialize-config []
      (println "initializing configurations..")
      (reset! app-configs (load-config (config-file)))

      (def application (config-value :ip-port-ext))
      (def ip-port-ext (config-value :ip-port-ext))

      (def db-host (config-value :db-host))
      (def db-port (config-value :db-port))
      (def db-name (config-value :db-name))
      (def db-user (config-value :db-user))
      (def db-pass (config-value :db-password))
      (def db-subprotocol (config-value :db-subprotocol))
      (def db-subname (config-value :db-subname))

      (def send-email-official-name (config-value :send-email-official-name))
      (def send-email-official (config-value :send-email-official))
      (def send-email-official-pass (config-value :send-email-official-pass))
      (def send-email-official-port (config-value :send-email-official-port))

      )

(defn initialize-db []
      (initialize-config)
      )
