(ns clj-pingback.server
  "This namespace contains provides functions for creating a pingback endpoint.
   What this server does not provide is a one size fits all endpoint; ever site
   has different rules about its resources and implementation. As a result this
   library is only able to provide utilities to make it easier to implement the
   end point.

   In all the functions implemented in this api target-uri refers to a url on
   the local site that has been refered to in a remote document. source-uri
   refers to that remote document."
  (:require [necessary-evil.core :as xml-rpc]
            [clj-http.client :as http])
  (:use [necessary-evil.fault :only [attempt-all fault]]))


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

(defn test-source
  "test-source attempts to connect to source-uri and determine if it contains
   a reference to target-uri."
  [source-ui target-uri]
  (attempt-all [] false))



(defn pingback-endpoint
  "pingback-endpoint provides the basic behaviour of the xml-rpc endpoint.
   Takes a function to do the actual work of handling the pingback."
  [handle-pingback]
  (xml-rpc.end-point
   {:pingback.ping (fn [source-uri target-uri]
                     (if (test-source source-uri target-uri)
                       true
                       (generic-failure)))}))
