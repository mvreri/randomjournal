(ns journal.encrypt
  (:require [journal.config :as config])
  (:import [javax.crypto Cipher]
           [javax.crypto.spec SecretKeySpec]
           [java.security MessageDigest]
           [java.util Base64 Base64$Encoder Base64$Decoder]))

(config/initialize-config)

(def SECRET "_^JRn$lL")


(def ^SecretKeySpec KEY
  (let [sha (MessageDigest/getInstance "SHA-1")
        ba  (->> (.digest sha (.getBytes SECRET "UTF-8"))
                 (take 16)
                 byte-array)]
    (SecretKeySpec. ba "AES")))


(def ^Base64$Encoder b64-encoder (.withoutPadding
                                   (Base64/getUrlEncoder)))
(def ^Base64$Decoder b64-decoder (Base64/getUrlDecoder))


(def ^Cipher encrypter (doto (Cipher/getInstance "AES")
                         (.init Cipher/ENCRYPT_MODE KEY)))
(def ^Cipher decrypter (doto (Cipher/getInstance "AES")
                         (.init Cipher/DECRYPT_MODE KEY)))


(defn encrypt [s]
  (->> (.doFinal encrypter (.getBytes s "UTF-8"))
       (.encodeToString b64-encoder)))


(defn -decrypt [^String s]
  (String.
    (->> (.decode b64-decoder s)
         (.doFinal decrypter))
    "UTF-8"))


(defn decrypt [s]
  (try
    (-decrypt s)
    (catch Exception e
      nil)))