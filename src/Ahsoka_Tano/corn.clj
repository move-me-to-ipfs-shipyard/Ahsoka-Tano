(ns Ahsoka-Tano.corn
  (:require
   [clojure.core.async :as Little-Rock
    :refer [chan put! take! close! offer! to-chan! timeout thread
            sliding-buffer dropping-buffer
            go >! <! alt! alts! do-alts
            mult tap untap pub sub unsub mix unmix admix
            pipe pipeline pipeline-async]]
   [clojure.java.io :as Wichita.java.io]
   [clojure.string :as Wichita.string]
   [cheshire.core :as Cheshire-Cat.core]

   [aleph.http :as Simba.http]
   [manifold.deferred :as Nala.deferred]
   [manifold.stream :as Nala.stream]
   [byte-streams :as Rafiki]

   [Ahsoka-Tano.seed])
  (:import
   (io.ipfs.api IPFS)
   (java.util.stream Stream)
   (java.util Base64)
   (java.io BufferedReader)
   (java.nio.charset StandardCharsets)))

(do (set! *warn-on-reflection* true) (set! *unchecked-math* true))

(def raw-stream-connection-pool (Simba.http/connection-pool {:connection-options {:raw-stream? true}}))

(defn encode-base64url-u
  [^String string]
  (-> (Base64/getUrlEncoder) (.withoutPadding)
      (.encodeToString (.getBytes string StandardCharsets/UTF_8)) (->> (str "u"))))

(defn decode-base64url-u
  [^String string]
  (-> (Base64/getUrlDecoder)
      (.decode (subs string 1))
      (String. StandardCharsets/UTF_8)))

(defn pubsub-sub
  [base-url topic message| cancel| ]
  (let [streamV (volatile! nil)]
    (->
     (Nala.deferred/chain
      (Simba.http/post (str base-url "/api/v0/pubsub/sub")
                       {:query-params {:arg topic}
                        :pool raw-stream-connection-pool})
      :body
      (fn [stream]
        (vreset! streamV stream)
        stream)
      #(Nala.stream/map Rafiki/to-string %)
      (fn [stream]
        (Nala.deferred/loop
         []
          (->
           (Nala.stream/take! stream :none)
           (Nala.deferred/chain
            (fn [message-string]
              (when-not (identical? message-string :none)
                (let [message (Cheshire-Cat.core/parse-string message-string true)]
                  #_(println :message message)
                  (put! message| message))
                (Nala.deferred/recur))))
           (Nala.deferred/catch Exception (fn [ex] (println ex)))))))
     (Nala.deferred/catch Exception (fn [ex] (println ex))))

    (go
      (<! cancel|)
      (Nala.stream/close! @streamV))
    nil))

(defn pubsub-pub
  [base-url topic message]
  (let []

    (->
     (Nala.deferred/chain
      (Simba.http/post (str base-url "/api/v0/pubsub/pub")
                       {:query-params {:arg topic}
                        :multipart [{:name "file" :content message}]})
      :body
      Rafiki/to-string
      (fn [response-string] #_(println :repsponse reresponse-stringsponse)))
     (Nala.deferred/catch
      Exception
      (fn [ex] (println ex))))

    nil))

(defn subscribe-process
  [{:keys [^String ipfs-api-multiaddress
           ^String ipfs-api-url
           frequency
           sub|
           cancel|
           id|]
    :as opts}]
  (let [ipfs (IPFS. ipfs-api-multiaddress)
        base-url ipfs-api-url
        topic (encode-base64url-u frequency)
        id (-> ipfs (.id) (.get "ID"))
        message| (chan (sliding-buffer 10))]
    (put! id| {:peer-id id})
    (pubsub-sub base-url  topic message| cancel|)

    (go
      (loop []
        (when-let [value (<! message|)]
          (put! sub| (merge value
                            {:message (-> (:data value) (decode-base64url-u) (read-string))}))
          #_(println (merge value
                            {:message (-> (:data value) (decode-base64url-u) (read-string))}))
          #_(when-not (= (:from value) id)

              #_(println (merge value
                                {:message (-> (:data value) (decode-base64url-u) (read-string))})))
          (recur))))

    #_(go
        (loop []
          (<! (timeout 2000))
          (pubsub-pub base-url topic (str {:id id
                                           :rand-int (rand-int 100)}))
          (recur)))))