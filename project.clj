(defproject oauthtest "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.51"]
                 [com.nimbusds/oauth2-oidc-sdk "5.9"]

                 [http-kit "2.1.18"]
                 [ring/ring-defaults "0.1.5"]
                 [compojure "1.4.0"]
                 [selmer "0.9.3"]
                 [enlive "1.1.6"]
                 ]
  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-figwheel "0.5.3-1"]]
  :src-paths ["src"]
  :cljsbuild {:builds
              [{:id "figwheel"
                :source-paths ["src"]
                :figwheel true
                :compiler {:output-dir "resources/public/js"
                           :output-to "resources/public/js/main.js"
                           :asset-path "js" ;; relative path from webserver root to where figwheel expects cljs stuff
                           :source-map true
                           :optimizations :none
                           :main "oauthtest.core"
                           :pretty-print true
                           :externs ["externs.js"]}}]})
