(defproject clj-pingback "0.2.0-SNAPSHOT"
  :description "An implementation of Pingback for clojure"
  :dependencies {org.clojure/clojure "1.4.0",
                 clj-http "0.5.8",
                 necessary-evil "2.0.0"}

  :profiles {:dev {:dependencies {ring/ring-jetty-adapter "1.2.0-SNAPSHOT"
                                  marginalia "0.7.0-SNAPSHOT"}
                   :plugins {lein-marginalia "0.7.0-SNAPSHOT"}}}
  
  :min-lein-version "2.0.0")
