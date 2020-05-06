package ca.bc.gov.open.oauth.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import net.minidev.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;

import ca.bc.gov.open.oauth.configuration.OauthProperties;
import ca.bc.gov.open.oauth.exception.OauthServiceException;
import ca.bc.gov.open.oauth.model.ValidationResponse;
import ca.bc.gov.open.oauth.util.AES256;
import ca.bc.gov.open.oauth.util.JwtTokenGenerator;

/**
 *
 * Oauth API Services Implementation class.
 *
 * @author shaunmillargov
 *
 */
@Service
@Configuration
@EnableConfigurationProperties(OauthProperties.class)
public class OauthServicesImpl implements OauthServices {

	@Autowired
	private OauthProperties oauthProps;

	@Autowired
	private JWTValidationServiceImpl tokenServices;

	private final Logger logger = LoggerFactory.getLogger(OauthServicesImpl.class);

	private static final String SCOPE_PREFIX = "BCSC_SCOPE_";
	private static final String RETURN_URI_PREFIX = "BCSC_RETURN_URI_";
	private static final String CLIENT_SECRET_PREFIX = "BCSC_SECRET_";
	private static final String PER_SECRET_PREFIX = "BCSC_PER_SECRET_";

	public URI getIDPRedirect(String returnUrl) throws URISyntaxException {

		logger.debug("Calling getIDPRedirect");

		// Client Id is set in the JWT filter
		String formattedClientId = oauthProps.getClientId().replace(".", "_").toUpperCase();

		// The authorisation endpoint of IDP the server
		URI authzEndpoint = new URI(oauthProps.getIdp() + oauthProps.getAuthorizePath());

		// The client identifier provisioned by the server
		ClientID clientID = new ClientID(oauthProps.getClientId());

		// The client callback URI, typically pre-registered with the server
		URI callback = new URI((returnUrl != null) ? returnUrl
				: System.getenv().getOrDefault(RETURN_URI_PREFIX + formattedClientId, ""));

		// Generate random state string for pairing the response to the request
		State state = new State();

		// The requested scope values for the token
		Scope scope = new Scope(System.getenv().getOrDefault(SCOPE_PREFIX + formattedClientId, ""));

		// Build the request
		AuthorizationRequest request = new AuthorizationRequest.Builder(new ResponseType(ResponseType.Value.CODE),
				clientID).scope(scope).state(state).redirectionURI(callback).endpointURI(authzEndpoint).build();

		// Use this URI to send the end-user's browser to the server
		return request.toURI();

	}

	public AccessTokenResponse getToken(String authCode, String returnUrl) throws OauthServiceException {

		logger.debug("Calling getToken");

		// Client Id is set in the JWT filter
		String formattedClientId = oauthProps.getClientId().replace(".", "_").toUpperCase();

		AuthorizationCode code = new AuthorizationCode(authCode);
		try {

			URI callback = new URI((returnUrl != null) ? returnUrl
					: System.getenv().getOrDefault(RETURN_URI_PREFIX + formattedClientId, ""));

			// The credentials to authenticate the client at the token endpoint
			ClientID clientID = new ClientID(oauthProps.getClientId());
			Secret clientSecret = new Secret(
					System.getenv().getOrDefault(CLIENT_SECRET_PREFIX + formattedClientId, ""));
			ClientAuthentication clientAuth = new ClientSecretBasic(clientID, clientSecret);

			AuthorizationGrant codeGrant = new AuthorizationCodeGrant(code, callback);

			// The IDP token endpoint
			URI tokenEndpoint = new URI(oauthProps.getIdp() + oauthProps.getTokenPath());

			// authorization_code == grant_type

			// Make the token request
			TokenRequest request = new TokenRequest(tokenEndpoint, clientAuth, codeGrant);

			TokenResponse response = TokenResponse.parse(request.toHTTPRequest().send());

			if (!response.indicatesSuccess()) {
				TokenErrorResponse errorResponse = response.toErrorResponse();
				throw new OauthServiceException(
						"Token Error Response from IdP server: " + errorResponse.toJSONObject().toJSONString());
			}

			// Respond with the complete token returned from the IdP.
			return response.toSuccessResponse();

		} catch (URISyntaxException e) {
			throw new OauthServiceException(e.getMessage(), e);
		} catch (ParseException e) {
			throw new OauthServiceException("Parse Exception", e);
		} catch (IOException e) {
			throw new OauthServiceException("Error comunicating with IdP server", e);
		}

	}

	public JSONObject getUserInfo(BearerAccessToken accessToken) throws OauthServiceException {

		try {

			// Build the IdP endpoint for user info data
			HTTPResponse httpResponse = new UserInfoRequest(new URI(oauthProps.getIdp() + oauthProps.getUserinfoPath()),
					accessToken).toHTTPRequest().send();

			// Parse the response
			UserInfoResponse userInfoResponse = UserInfoResponse.parse(httpResponse);

			// The request failed, e.g. due to invalid or expired token
			if (!userInfoResponse.indicatesSuccess()) {
				throw new OauthServiceException("Invalid response returned from server for userinfo request.");
			}

			// Extract the claims
			return userInfoResponse.toSuccessResponse().getUserInfoJWT().getJWTClaimsSet().toJSONObject();
		} catch (ParseException e) {
			throw new OauthServiceException("Error parsing userinfo data returned from server. ", e);
		} catch (Exception e) {
			throw new OauthServiceException(e.getMessage(), e);
		}

	}

	/*
	 * 
	 * Uses authorization code provided back from call to /authorize to generate
	 * access token from IdP.
	 * 
	 * Responds to SPA with new JWT (complete with userInfo and encrypted IdP
	 * token).
	 * 
	 */
	public ResponseEntity<String> login(String authCode, String returnUrl) throws OauthServiceException {
		AccessTokenResponse token = null;
		try {
			token = getToken(authCode, returnUrl);
		} catch (Exception e) {
			logger.error("Error while calling Oauth2 /token endpoint. ", e);
			return new ResponseEntity<>(OauthServiceException.OAUTH_FAILURE_RESPONSE, HttpStatus.FORBIDDEN);
		}

		// Validate tokens received from BCSC.
		logger.debug("Validating ID token received from BCSC...");
		ValidationResponse valResp = tokenServices
				.validateBCSCIDToken((String) token.toSuccessResponse().getCustomParameters().get("id_token"));
		if (!valResp.isValid()) {
			logger.error("ID token failed to validate. Error {}", valResp.getMessage());
			return new ResponseEntity<>(OauthServiceException.OAUTH_FAILURE_RESPONSE, HttpStatus.FORBIDDEN);
		}

		// Fetch corresponding Userinfo from the IdP server.
		JSONObject userInfo = null;
		try {
			userInfo = getUserInfo((BearerAccessToken) token.toSuccessResponse().getTokens().getAccessToken());
		} catch (OauthServiceException e) {
			logger.error("Error fetching userinfo:", e);
			return new ResponseEntity<>(OauthServiceException.OAUTH_FAILURE_RESPONSE, HttpStatus.FORBIDDEN);
		}

		// Client Id is set in the JWT filter
		String formattedClientId = oauthProps.getClientId().replace(".", "_").toUpperCase();

		// Encrypt the Access Token received from the IdP. This token has a 1 hour
		// expiry time on it and must be decrypted and used for subsequent calls back to
		// the API.
		String encryptedAccessToken = null;
		try {
			encryptedAccessToken = AES256.encrypt(token.getTokens().getBearerAccessToken().getValue(),
					System.getenv().getOrDefault(PER_SECRET_PREFIX + formattedClientId, ""));
		} catch (Exception e) {
			logger.error("Error encrypting token:", e);
			return new ResponseEntity<>(OauthServiceException.OAUTH_FAILURE_RESPONSE, HttpStatus.FORBIDDEN);
		}

		// Send the new FE JWT in the response body to the caller.
		String feTokenResponse = JwtTokenGenerator.generateFEAccessToken(userInfo, encryptedAccessToken,
				oauthProps.getJwtSecret(), oauthProps.getJwtExpiry(), oauthProps.getJwtAuthorizedRole());
		return new ResponseEntity<>(feTokenResponse, HttpStatus.OK);
	}
}
