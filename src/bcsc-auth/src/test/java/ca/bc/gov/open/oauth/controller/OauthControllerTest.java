package ca.bc.gov.open.oauth.controller;

import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;

import net.minidev.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ca.bc.gov.open.oauth.configuration.OauthProperties;
import ca.bc.gov.open.oauth.exception.OauthServiceException;
import ca.bc.gov.open.oauth.service.OauthServicesImpl;

import static org.mockito.ArgumentMatchers.any;

/**
 * Tests for oauth controller
 * 
 * @author sivakaruna
 *
 */
class OauthControllerTest {

	@Mock
	OauthServicesImpl oauthServices;

	@Mock
	OauthProperties oauthProperties;

	@InjectMocks
	OauthController oauthController = new OauthController();

	JSONObject userInfo;

	@BeforeEach
	public void initMocks() {
		MockitoAnnotations.initMocks(this);
		Mockito.when(oauthProperties.getJwtSecret()).thenReturn("secret");
		Mockito.when(oauthProperties.getJwtExpiry()).thenReturn(300);
		Mockito.when(oauthProperties.getJwtAuthorizedRole()).thenReturn("role");
		userInfo = new JSONObject();
		userInfo.put("sub", "test");
	}

	@DisplayName("Success - getBCSCUrl oauth controller default")
	@Test
	void testDefaultGetBCSCUrlSuccess() throws OauthServiceException, URISyntaxException {
		when(oauthServices.getIDPRedirect(null)).thenReturn(new URI("test"));
		ResponseEntity<String> response = oauthController.getBCSCUrl(null);
		Assertions.assertEquals("test", response.getBody());
		Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
	}

	@DisplayName("Success - getBCSCUrl oauth controller")
	@Test
	void testGetBCSCUrlSuccess() throws OauthServiceException, URISyntaxException {
		when(oauthServices.getIDPRedirect("TEST")).thenReturn(new URI("test"));
		ResponseEntity<String> response = oauthController.getBCSCUrl("TEST");
		Assertions.assertEquals("test", response.getBody());
		Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
	}

	@DisplayName("Error - getBCSCUrl oauth controller")
	@Test
	void testGetBCSCUrlError() throws OauthServiceException, URISyntaxException {
		when(oauthServices.getIDPRedirect(null)).thenReturn(null);
		Assertions.assertThrows(OauthServiceException.class, () -> {
			oauthController.getBCSCUrl(null);
		});
	}

	@DisplayName("Success - login oauth controller default")
	@Test
	void testDefaultLoginSuccess() throws OauthServiceException, URISyntaxException {

		when(oauthServices.login(any(), any())).thenReturn(new ResponseEntity<>("test", HttpStatus.OK));
		ResponseEntity<String> response = oauthController.login("test", null);
		Assertions.assertNotNull(response);
	}

	@DisplayName("Success - login oauth controller other")
	@Test
	void testOtherLoginSuccess() throws OauthServiceException, URISyntaxException {

		when(oauthServices.login(any(), any())).thenReturn(new ResponseEntity<>("test", HttpStatus.OK));
		ResponseEntity<String> response = oauthController.login("test", "TEST");
		Assertions.assertNotNull(response);
	}

	@DisplayName("Error - login oauth controller")
	@Test
	void testLoginError1() throws OauthServiceException, URISyntaxException {
		when(oauthServices.login(any(), any())).thenThrow(new OauthServiceException("error"));
		ResponseEntity<String> response = oauthController.login("code", null);
		Assertions.assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
	}
}
