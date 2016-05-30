(ns oauthtest.core
  (:require [oauthtest.handlers :as h]
            [org.httpkit.server :as hs]
            [compojure.core :refer [defroutes context GET POST]]
            [compojure.handler :refer [site]]))


(def conn-data {:client-id     "817304564499-tg8htvvunf7ds8rbp0jc30ijm20odo7k.apps.googleusercontent.com"
                :client-secret "dFlqsKkhk0oA1se3zk5AUKYJ"
                :provider-url  "https://accounts.google.com/o/oauth2/v2/auth"
                :redirect-url  "http://localhost:8899/auth/code"
                :scopes        ["openid" "email"]
                :issuer        "https://accounts.google.com"
                :token-url     "https://www.googleapis.com/oauth2/v4/token"
                :jwks-url      "https://www.googleapis.com/oauth2/v3/certs"})


(def server (atom nil))


(comment (defn get-env []
           {:clientid (System/getenv "CLIENTID")
            :clientsecret (System/getenv "CLIENTSECRET")
            :provideruri (System/getenv "PROVIDERURI")
            :redirecturi (System/getenv "REDIRECTURI")}))

(defroutes auth-routes
  (context "/auth" []
           (GET "/"     [] (h/make-auth-handler (:client-id conn-data)
                                                (:provider-url conn-data)
                                                (:redirect-url conn-data)
                                                (:scopes conn-data)))
           (GET "/code" [] (h/make-code-handler (:client-id conn-data)
                                                (:client-secret conn-data)
                                                (:issuer conn-data)
                                                (:token-url conn-data)
                                                (:redirect-url conn-data)
                                                (:jwks-url conn-data)))))

(defn start []
  (reset! server (hs/run-server (site #'auth-routes)
                                {:port 8899})))

(defn stop []
  (when-not (nil? @server)
    (@server :timeout 5000)
    (reset! server nil)))
