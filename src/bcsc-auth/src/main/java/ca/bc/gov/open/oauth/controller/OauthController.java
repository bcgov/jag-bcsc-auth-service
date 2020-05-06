package ca.bc.gov.open.oauth.controller;

import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ca.bc.gov.open.oauth.configuration.OauthProperties;
import ca.bc.gov.open.oauth.service.OauthServicesImpl;
import ca.bc.gov.open.oauth.exception.OauthServiceException;
import ch.qos.logback.classic.Logger;

/**
 * 
 * Oauth Controller class.
 * 
 * Provides OIDC endpoints serving the eCRC front end.
 * 
 * @author shaunmillargov
 *
 */
@Configuration
@EnableConfigurationProperties(OauthProperties.class)
@RestController
public class OauthController {

	@Autowired
	private OauthServicesImpl oauthServices;

	private final Logger logger = (Logger) LoggerFactory.getLogger(OauthController.class);

	public static final String REQUEST_ENDPOINT = "request.endpoint";

	@ResponseStatus(code = HttpStatus.FOUND)
	@GetMapping(value = "/initiateBCSCAuthentication")
	public ResponseEntity<String> getBCSCUrl(@RequestParam(required = false) String returnUrl)
			throws OauthServiceException {
		MDC.put(REQUEST_ENDPOINT, "getBCSCUrl");
		logger.info("BCSC URL request received [{}]");

		try {
			return new ResponseEntity<>(oauthServices.getIDPRedirect(returnUrl).toString(), HttpStatus.OK);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new OauthServiceException("Configuration Error");
		} finally {
			MDC.remove(REQUEST_ENDPOINT);
		}
	}

	@GetMapping(value = "/getToken")
	public ResponseEntity<String> login(@RequestParam(name = "code", required = true) String authCode,
			@RequestParam(required = false) String returnUrl) throws OauthServiceException {
		MDC.put(REQUEST_ENDPOINT, "login");
		logger.info("Login URL request received {}");

		try {
			return oauthServices.login(authCode,returnUrl);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return new ResponseEntity<>(OauthServiceException.OAUTH_FAILURE_RESPONSE, HttpStatus.FORBIDDEN);
		} finally {
			MDC.remove(REQUEST_ENDPOINT);
		}
	}

}