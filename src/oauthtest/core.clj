(ns oauthtest.core
  (:require [oauthtest.handlers :as h]
            [org.httpkit.server :as hs]
            [compojure.core :refer [defroutes context GET POST]]
            [compojure.handler :refer [site]]))


(defonce server (atom nil))

(defroutes auth-routes
  (context "/auth" []
           (GET "/"     [] (h/make-auth-handler "817304564499-tg8htvvunf7ds8rbp0jc30ijm20odo7k.apps.googleusercontent.com"
                                                "https://accounts.google.com/o/oauth2/v2/auth"
                                                "http://localhost:8899/auth/code"
                                                ["openid" "email"]))
           (GET "/code" [] h/code-handler)))

(defn start []
  (reset! server (hs/run-server (site #'auth-routes)
                                {:port 8899})))

(defn stop []
  (when-not (nil? @server)
    (@server :timeout 5000)
    (reset! server nil)))
