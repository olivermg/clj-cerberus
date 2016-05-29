(ns oauthtest.core
  (:require [oauthtest.handlers :as h]
            [org.httpkit.server :as hs]
            [compojure.core :refer [defroutes context GET POST]]
            [compojure.handler :refer [site]]))


(defonce server (atom nil))

(comment (defn get-env []
           {:clientid (System/getenv "CLIENTID")
            :clientsecret (System/getenv "CLIENTSECRET")
            :provideruri (System/getenv "PROVIDERURI")
            :redirecturi (System/getenv "REDIRECTURI")}))

(defroutes auth-routes
  (context "/auth" []
           (GET "/"     [] (h/make-auth-handler "817304564499-tg8htvvunf7ds8rbp0jc30ijm20odo7k.apps.googleusercontent.com"
                                                "https://accounts.google.com/o/oauth2/v2/auth"
                                                "http://localhost:8899/auth/code"
                                                ["openid" "email"]))
           (GET "/code" [] (h/make-code-handler "817304564499-tg8htvvunf7ds8rbp0jc30ijm20odo7k.apps.googleusercontent.com"
                                                "http://www.googleapis.com"
                                                "http://localhost:8899/auth/code"))))

(defn start []
  (reset! server (hs/run-server (site #'auth-routes)
                                {:port 8899})))

(defn stop []
  (when-not (nil? @server)
    (@server :timeout 5000)
    (reset! server nil)))
