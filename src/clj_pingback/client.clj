(ns clj-pingback.client
  "This namespace contains provides functions for sending pingbacks.
   The pingback function is the common case that most users are interested in."
  (:require [necessary-evil.core :as xml-rpc]
            [clj-http.client :as http]))

(def link-pattern #"<link rel=\"pingback\" href=\"([^\"]+)\" ?/?>")

(defn discover-pingback-endpoint
  "Given a uri, discover-pingback-endpoint will return the XML-RPC endpoint for
   that resource or nil."
  [uri]
  (let [response (http/get uri {:throw-exceptions false})]
    (if-let [url (get-in response [:headers "x-pingback"])]
      url
      (when-let [[_ url] (re-find link-pattern (:body response))] url))))

(defn pingback-single
  "pingback-single will syncronously attempt a single pingback call,
   and handle the result"
  [source-uri target-uri]
  (when-let [end-point (discover-pingback-endpoint target-uri)]
      (xml-rpc/call end-point :pingback.ping source-uri target-uri)))

(defn pingback
  "pingback will attempt to send a pingback message to each uri in the
   target-uris sequence originating from the source-uri.

   All pingback calls occur in parallel, but the pingback call itself is
   blocking. Finally a map is produced with each target uri mapped to its result.

   Each unique uri in the targets-uri seq is only sent one pingback."
  [source-uri target-uris]
  (into {} (pmap (fn [t] [t (pingback-single source-uri t)])
                 (set target-uris))))

