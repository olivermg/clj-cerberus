(ns oauthtest.core
  (:require [oauthtest.handlers :as h]
            [org.httpkit.server :as hs]
            [compojure.core :refer [defroutes context GET POST]]
            [compojure.handler :refer [site]]))


(def conn-data {;;; client-id & client-secret: Your OP will give these to you after registration
                ;;; (contrary to this example, you probably want to keep client-secret private, so
                ;;; you'll probably want to get it from some config instead of hardcoding it in
                ;;; your source code):
                :client-id     "817304564499-tg8htvvunf7ds8rbp0jc30ijm20odo7k.apps.googleusercontent.com"
                :client-secret "dFlqsKkhk0oA1se3zk5AUKYJ"

                ;;; redirect-url: Point this to the url you want users to be redirected to after they've
                ;;;   completed authentication at the OP (note that you might have to tell your OP a list
                ;;;   of valid redirect-urls):
                :redirect-url  "http://localhost:8899/auth/code"
                ;;; scopes: Set this to desired OpenID Connect scopes that your app needs to gain access to:
                :scopes        ["openid" "email"]

                ;;; Get these URLs from your OP. For the example of Google, there is a discovery document at
                ;;;   https://accounts.google.com/.well-known/openid-configuration
                ;;; For more info, you want to read your OP's docs, e.g.
                ;;;   https://developers.google.com/identity/protocols/OpenIDConnect#discovery
                :issuer        "https://accounts.google.com"
                :provider-url  "https://accounts.google.com/o/oauth2/v2/auth"
                :token-url     "https://www.googleapis.com/oauth2/v4/token"
                :jwks-url      "https://www.googleapis.com/oauth2/v3/certs"
                })


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
