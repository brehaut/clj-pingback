# clj-pingback

An implementation of the [Pingback API](http://www.hixie.ch/specs/pingback/pingback-1.0) for clojure. This library is built ontop of [necessary-evil](https://github.com/brehaut/necessary-evil): this is particularly relevant if you wish to implement a server endpoint as some understanding of how `necessary-evil` interacts with the [Ring http library](https://github.com/ring-clojure/ring).

Due to dependancy on necessary-evil, clj-pingback requires Java 6+ and clojure 1.2.1+.

## Usage

### Client

```clojure
(require '[clj-pingback.client :refer [pingback]])
```

The client is one function, `pingback`. This function takes a source URI 
(the URI of the page that is the source of the pingback) and a seq of all 
the target URIs that that page references. For example, it might be:

```clojure
(pingback "http://example.com/foo" 
          ["http://example.net/blog/10"
           "http://example.org/2012/12/8/example-post"])
```

In this case, both `http://example.net/blog/10` and `http://example.org/2012/12/8/example-post` will recieve pingbacks from `http://example.com/foo`.

`pingback` returns a map of target URIs mapping to the result. The result will be one of:

 * *`nil`*: The URI does not provide an endpoint for PingBacks.
 * *A string*: The Pingback was successfully recieved
 * *An XML-RPC fault record*: The pingback did not succeed. Consult the [Pingback spec](http://www.hixie.ch/specs/pingback/pingback-1.0#TOC3) for fault codes.

Note that while pingback sends out the pings concurrently, the function blocks until all the results are collated. 

### Server

Implementing Pingback in a web application is fairly straight forward. The `pingback-endpoint` handles all the generic behaviour. It takes an implementation of the `Pingbackable` protocol to delegate to for the specific details of sites backend. This creates an xml-rpc endpoint as a Ring handler function. Embed this into your application as you would any other Ring handler. See the necessary-evil documention for examples. 

Consult the docstring on `clj-pingback.server/Pingbackable' for specifics on implementing 
you own backend.

The following is a trivial, but useless, example:

```clojure
(require '[clj-pingback.server :refer [pingback-endpoint
                                       Pingbackable]]
         '[ring.adapter.jetty :refer [run-jetty])

(def ep (pingback-endpoint 
          (reify Pingbackable
                 (target-uri-valid? 
                    [this target-uri] 
                    true)

                 (register-pingback 
                    [this source-uri target-uri]
                    true))))

(run-jetty #'ep {:join? false :port 3000})
```

## License

Copyright (C) 2010, 2012 Andrew Brehaut

Distributed under the Eclipse Public License, the same as Clojure.
