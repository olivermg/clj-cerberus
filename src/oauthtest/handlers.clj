[](ns oauthtest.handlers
  (:refer-clojure :rename {send send-clj})
  (:require [ring.util.request :as r])
  (:import [com.nimbusds.openid.connect.sdk AuthenticationRequest]
           [com.nimbusds.openid.connect.sdk AuthenticationResponseParser]
           [com.nimbusds.openid.connect.sdk Nonce]
           [com.nimbusds.oauth2.sdk AuthorizationCode]
           [com.nimbusds.oauth2.sdk AuthorizationCodeGrant]
           [com.nimbusds.oauth2.sdk Scope]
           [com.nimbusds.oauth2.sdk ResponseType]
           [com.nimbusds.oauth2.sdk TokenRequest]
           [com.nimbusds.oauth2.sdk.id ClientID]
           [com.nimbusds.oauth2.sdk.id State]
           [java.net URI]))


(defn auth-request [client-id provider-uri redirect-uri scopes]
  (AuthenticationRequest. (URI. provider-uri)
                          (ResponseType. (into-array ["code"]))
                          (Scope. (into-array scopes))
                          (ClientID. client-id)
                          (URI. redirect-uri)
                          (State. "statefoo")
                          (Nonce. "noncebar")))

(defn send [auth-req]
  (-> auth-req .toHTTPRequest .send))

(defn make-auth-handler [client-id provider-uri redirect-uri scopes]
  (let [areq (auth-request client-id provider-uri redirect-uri scopes)
        code-uri (.toURI areq)]
    (fn [req]
      {:status 302
       :headers {"Location" code-uri}})))

(defn make-code-handler [client-id token-uri redirect-uri]
  (fn [req]
    (println req)
    (println (r/request-url req))
    (let [ares (AuthenticationResponseParser/parse (URI. (r/request-url req)))]
      (println ares))
    (let [acode (AuthorizationCode. (-> req :params :code))
          grant (AuthorizationCodeGrant. acode (URI. redirect-uri))
          treq (TokenRequest. (URI. token-uri)
                              (ClientID. client-id)
                              grant)]
      (println treq))
    {:status 200
     :headers {"Content-type" "text/plain"}
     :body "wurstschinken"}))


(comment (def ar1 (auth-request "817304564499-tg8htvvunf7ds8rbp0jc30ijm20odo7k.apps.googleusercontent.com"
                                "https://accounts.google.com/o/oauth2/v2/auth"
                                "http://localhost:3449/code.html"
                                ["openid" "email"]))
         (send ar1))
