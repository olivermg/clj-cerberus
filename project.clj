(defproject clj-cerberus "0.1.0-SNAPSHOT"
  :description "For simply using OpenID Connect in Clojure"
  :url "http://github.com/olivermg/clj-cerberus"
  :license {:name "The MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.51"]
                 [com.nimbusds/oauth2-oidc-sdk "5.9.1"]
                 [cheshire "5.6.1"]

                 [http-kit "2.1.18"]
                 [ring/ring-defaults "0.1.5"]
                 [compojure "1.4.0"]
                 ]
;;;  :plugins [[lein-cljsbuild "1.1.3"]
;;;            [lein-figwheel "0.5.3-1"]]
  :src-paths ["src"]
;;;  :cljsbuild {:builds
;;;              [{:id "figwheel"
;;;                :source-paths ["src"]
;;;                :figwheel true
;;;                :compiler {:output-dir "resources/public/js"
;;;                           :output-to "resources/public/js/main.js"
;;;                           :asset-path "js" ;; relative path from webserver root to where figwheel expects cljs stuff
;;;                           :source-map true
;;;                           :optimizations :none
;;;                           :main "oauthtest.core"
;;;                           :pretty-print true
;;;                           :externs ["externs.js"]}}]}
  )
