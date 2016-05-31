(ns oauthtest.handlers
  (:require [cheshire.core :as ch]
            [ring.util.codec :as rc]
            [clojure.string :as str])
  (:import [com.nimbusds.openid.connect.sdk AuthenticationRequest]
           [com.nimbusds.openid.connect.sdk AuthenticationResponseParser]
           [com.nimbusds.openid.connect.sdk Nonce]
           [com.nimbusds.openid.connect.sdk.validators IDTokenValidator]
           [com.nimbusds.oauth2.sdk AuthorizationCode]
           [com.nimbusds.oauth2.sdk AuthorizationCodeGrant]
           [com.nimbusds.oauth2.sdk AuthorizationResponse]
           [com.nimbusds.oauth2.sdk Scope]
           [com.nimbusds.oauth2.sdk ResponseType]
           [com.nimbusds.oauth2.sdk TokenRequest]
           [com.nimbusds.oauth2.sdk TokenResponse]
           [com.nimbusds.oauth2.sdk AccessTokenResponse]
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

(defn- decode-jwt [jwt expected-nonce expected-issuer client-id jwks-uri]
  (let [jwtvalidator (IDTokenValidator. (Issuer. expected-issuer)
                                        (ClientID. client-id)
                                        JWSAlgorithm/RS256
                                        (URL. jwks-uri))
        jwt-parsed (JWTParser/parse jwt)
        claims (.validate jwtvalidator jwt-parsed expected-nonce)
        cljclaims (ch/parse-string (-> claims .toJSONObject .toJSONString))]
    cljclaims))

(defn- response-to-map [getfn]
  {:access-token (getfn "access_token")
   :token-type (getfn "token_type")
   :expires-in (getfn "expires_in")
   :scope (getfn "scope")})

(defn- decode-json-response [res client-id expected-nonce expected-issuer jwks-uri]
  (let [jsonres (.getContentAsJSONObject res)
        jwt (.get jsonres "id_token")
        cljres (response-to-map #(.get jsonres %))]
    (if jwt
      (merge cljres
             {:id-token {:original jwt
                         :verified true
                         :payload (decode-jwt jwt expected-nonce expected-issuer
                                              client-id jwks-uri)}})
      cljres)))

(defn- decode-wwwform-response [res]
  (let [dec (rc/form-decode (.getContent res))]
    (println dec)
    (response-to-map #(get dec %))))

(defn- decode-response [res client-id expected-nonce expected-issuer jwks-uri]
  (if (.indicatesSuccess res)
    ;;; we have an SuccessResponse here
    (let [[ctype charset] (str/split (str/lower-case (.getContentType res))
                                     #"[\s;\s]+")]
      (case ctype
        "application/json" (decode-json-response res client-id expected-nonce expected-issuer jwks-uri)
        "application/x-www-form-urlencoded" (decode-wwwform-response res)))
    ;;; we have a TokenErrorResponse here
    (handle-nimbus-error res)))

(defn make-token-request [request-url client-id client-secret state nonce issuer token-uri redirect-uri jwks-uri]
  (let [ares (AuthenticationResponseParser/parse (URI. request-url))
        _ (println "ARES")
        treq (auth-response->token-request ares state client-id client-secret token-uri redirect-uri)
        _ (println "TREQ" (-> treq .toHTTPRequest))
        tres (-> treq .toHTTPRequest .send)
        ;;_ (println "TRES1" tres1 (.getHeaders tres1) (.getStatusCode tres1) (.getContent tres1))
        ;;tres (AccessTokenResponse/parse tres1)
        _ (println "TRES")
        ;;_ (println (rc/form-decode (.getContent tres)))
        claims (decode-response tres client-id nonce issuer jwks-uri)
        _ (println "CLAIMS")]
    claims))
