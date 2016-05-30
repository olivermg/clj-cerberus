(ns oauthtest.handlers
  (:require [cheshire.core :as ch])
  (:import [com.nimbusds.openid.connect.sdk AuthenticationRequest]
           [com.nimbusds.openid.connect.sdk AuthenticationResponseParser]
           [com.nimbusds.openid.connect.sdk Nonce]
           [com.nimbusds.openid.connect.sdk.validators IDTokenValidator]
           [com.nimbusds.oauth2.sdk AuthorizationCode]
           [com.nimbusds.oauth2.sdk AuthorizationCodeGrant]
           [com.nimbusds.oauth2.sdk Scope]
           [com.nimbusds.oauth2.sdk ResponseType]
           [com.nimbusds.oauth2.sdk TokenRequest]
           [com.nimbusds.oauth2.sdk TokenResponse]
           [com.nimbusds.oauth2.sdk.auth ClientAuthentication]
           [com.nimbusds.oauth2.sdk.auth ClientAuthenticationMethod]
           [com.nimbusds.oauth2.sdk.auth ClientSecretBasic]
           [com.nimbusds.oauth2.sdk.auth Secret]
           [com.nimbusds.oauth2.sdk.id ClientID]
           [com.nimbusds.oauth2.sdk.id State]
           [com.nimbusds.oauth2.sdk.id Issuer]
           [com.nimbusds.jwt JWTParser]
           [com.nimbusds.jose JWSAlgorithm]
           [java.net URI]
           [java.net URL]))


(defn- handle-error [msg info]
  (println msg info)
  (throw (ex-info msg info)))

(defn- handle-nimbus-error [res]
  (let [err (.getErrorObject res)
        info {:code (.getCode err)
              :httpcode (.getHTTPStatusCode err)
              :message (.getDescription err)
              :uri (.getURI err)}]
    (handle-error (str "ERROR: " (:message info))
                  info)))


(defn make-auth-request [client-id provider-uri redirect-uri scopes]
  (let [state (State.)
        nonce (Nonce.)
        req (AuthenticationRequest. (URI. provider-uri)
                                    (ResponseType. (into-array ["code"]))
                                    (Scope. (into-array scopes))
                                    (ClientID. client-id)
                                    (URI. redirect-uri)
                                    state
                                    nonce)]
    {:state state
     :nonce nonce
     :request req
     :uri (.toURI req)}))


(defn- auth-response->token-request [res expected-state client-id client-secret token-uri redirect-uri]
  (if (.indicatesSuccess res)
    ;;; we have an AuthenticationSuccessResponse here
    (let [state (.getState res)]
      (if (= state expected-state)
        (let [acode (.getAuthorizationCode res)
              grant (AuthorizationCodeGrant. acode (URI. redirect-uri))
              clientauth (ClientSecretBasic. (ClientID. client-id)
                                             (Secret. client-secret))]
          (TokenRequest. (URI. token-uri)
                         clientauth
                         grant))
        (handle-error "INVALID STATE" {:state state :expected-state expected-state})))
    ;;; we have an AuthorizationErrorResponse here
    (handle-nimbus-error res)))

(defn- token-response->claims [res expected-nonce expected-issuer client-id jwks-uri]
  (if (.indicatesSuccess res)
    ;;; we have an AccessTokenResponse here
    (let [jsonres (.toJSONObject res)
          cljres {:id-token (.get jsonres "id_token")
                  :access-token (.get jsonres "access_token")
                  :token-type (.get jsonres "token_type")
                  :expires-in (.get jsonres "expires_in")}
          jwtvalidator (IDTokenValidator. (Issuer. expected-issuer)
                                          (ClientID. client-id)
                                          JWSAlgorithm/RS256
                                          (URL. jwks-uri))
          jwt (JWTParser/parse (:id-token cljres))
          claims (.validate jwtvalidator jwt expected-nonce)
          cljclaims (ch/parse-string (-> claims .toJSONObject .toJSONString))]
      (assoc cljres :id-token-payload cljclaims))
    ;;; we have a TokenErrorResponse here
    (handle-nimbus-error res)))

(defn make-token-request [request-url client-id client-secret state nonce issuer token-uri redirect-uri jwks-uri]
  (let [tres (AuthenticationResponseParser/parse (URI. request-url))
        treq (auth-response->token-request tres state client-id client-secret token-uri redirect-uri)
        tres (TokenResponse/parse (-> treq .toHTTPRequest .send))
        claims (token-response->claims tres nonce issuer client-id jwks-uri)]
    claims))
