(ns clj-pingback.server
  "This namespace contains provides functions for creating a pingback endpoint.
   What this server does not provide is a one size fits all endpoint; ever site
   has different rules about its resources and implementation. To implement an
   endpoint you thus need to implement the Pingable protocol for your backend.

   In all the functions implemented in this api target-uri refers to a url on
   the local site that has been refered to in a remote document. source-uri
   refers to that remote document."
  (:require [necessary-evil.core :as xml-rpc]
            [clj-http.client :as http])
  (:use [necessary-evil.fault :only [attempt-all fault]]))


(defprotocol Pingbackable
  "This protocol defines the functions needed for various backends to plug into 
   the pingback endpoint to support recieving pingbacks.

   These functions may return necessary-evil.fault/fault's to indicated failure.
   Faults codes specified by the pingback spec have constructor functions below."
  
  (target-uri-valid?
    [this target-uri]
    "target-uri-valid? is used to determine if this is something that can be pinged.
     It uses the criteria of the looser fault code 0×0021 (target-isnt-pingback-enabled)
     rather than the specific 'does not exist' fault code 0×0020 (target-doesnt-exist).

     The pingback-handler will call this method and if the result is false, the
     appropriate fault will be returned. If true is returned, then processing
     will continue.

     If you are able to satisfy the more strict conditions of the 0×0020
     (target-doesnt-exist) fault you may return that instead.")

  (register-pingback
    [this source-uri target-uri]
    "Register a pingback from the source-uri to the target-uri.
     If the source-uri has already reigstered a pingback on target-uri you may opt
     to return fault code 0×0030 (pingback-already-registered)."))


;; Base fault code functions
;;
;; These fault codes are specified at in section 3 'XML-RPC Interface'
;; of the pingback spec at http://www.hixie.ch/specs/pingback/pingback-1.0#TOC3

(defn- paren [s & rest] (let [s (str s)] (when (seq s) (apply str "(" s ")" rest))))
(defn- colon-prefix [s] (let [s (str s)] (when (seq s) (str ": " s))))

(defn generic-fault
  ([] (generic-fault nil))
  ([message] (fault 0 (str "Generic fault" (colon-prefix message)))))

(defn source-doesnt-exist
  ([] (source-doesnt-exist nil))
  ([source-uri]
     (fault 16 (str "The source URI " (paren source-uri " ") "does not exist."))))

(defn source-doesnt-reference-target
  ([] (source-doesnt-reference-target nil nil))
  ([source-uri target-uri]
     (fault 17 (str "The source URI "
                    (paren source-uri " ")
                    "does not contain a link to the target URI "
                    (paren target-uri)
                    ", and so cannot be used as a source."))))

(defn target-doesnt-exist
  ([] (target-doesnt-exist nil))
  ([target-uri]
     (fault 32 (str "The target URI does not exist" (paren target-uri)))))

(defn target-isnt-pingback-enabled
  ([] (target-isnt-pingback-enabled nil))
  ([target-uri]
     (fault 33 (str "The specified target URI "
                    (paren target-uri " ")
                    "cannot be used as a target. It either doesn't exist, or it is not a "
                    "pingback-enabled resource. For example, on a blog, typically only "
                    "permalinks are pingback-enabled, and trying to pingback the home page, "
                    "or a set of posts, will fail with this error."))))

(defn pingback-already-registered
  [] (fault 48 "The pingback has already been registered"))

(defn access-denied
  [] (fault 49 "Access denied"))

(defn communication-failure
  [] (fault 50 "The server could not communicate with an upstream server, or received an error from an upstream server, and therefore could not complete the request"))

;; Pingback behaviour

(defn is-valid-pingback?
  "is-valid-pingback attempts to connect to source-uri and determine if it contains
   a reference to target-uri."
  [pingbackable source-uri target-uri]
  (attempt-all [resp    (try (http/get source-uri {:throw-exceptions false})
                             (catch java.net.UnknownHostException e
                               (source-doesnt-exist source-uri)))
                
                content (condp == (:status resp)
                               200 (:body resp)
                               404 (source-doesnt-exist source-uri)
                               410 (source-doesnt-exist source-uri)
                               (communication-failure))
                
                _       (when-not (>= (.indexOf content target-uri) 0)
                          (source-doesnt-reference-target source-uri
                                                          target-uri))
                
                _       (when-not (target-uri-valid? pingbackable target-uri)
                          (target-isnt-pingback-enabled target-uri))]
               true))



(defn pingback-endpoint
  "pingback-endpoint provides the basic behaviour of the xml-rpc endpoint.

   pingbackable must implement the Pingbackable protocol."
  [pingbackable]
  (xml-rpc/end-point
   {:pingback.ping (fn [source-uri target-uri]
                     (attempt-all [_ (is-valid-pingback? pingbackable source-uri target-uri)
                                   _ (register-pingback pingbackable source-uri target-uri)]
                                  "Pingback successfully registered"))}))

