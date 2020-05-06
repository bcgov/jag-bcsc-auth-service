package ca.bc.gov.open.oauth.service;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

import net.minidev.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;

import ca.bc.gov.open.oauth.configuration.OauthProperties;
import ca.bc.gov.open.oauth.exception.OauthServiceException;
import ca.bc.gov.open.oauth.model.ValidationResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests for oauth service
 * 
 * @author sivakaruna
 *
 */
class OauthServicesImplTest {

	private final String jsonTokenSuccessResp = "{\"access_token\":\"abc.abc.abc\","
			+ "\"refresh_token\":\"def.def.def\"," + "\"scope\":\"scope\"," + "\"id_token\":\"ghi.ghi.ghi\","
			+ "\"token_type\":\"Bearer\"}";
	private final String jsonTokenErrorResp = "{\"refresh_token\":\"def.def.def\"," + "\"scope\":\"scope\","
			+ "\"id_token\":\"ghi.ghi.ghi\"," + "\"token_type\":\"Bearer\"}";

	private final String jwtSuccessResp = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0IiwiYmlydGhkYXRlIjoidGVzdCIsImdlbmRlciI6ImZlbWFsZSIsImlzcyI6InRlc3QiLCJnaXZlbl9uYW1lIjoiQ1JDIiwiZ2l2ZW5fbmFtZXMiOiJDUkMgdGVzdCIsImRpc3BsYXlfbmFtZSI6IkNSQyB0ZXN0IiwiYXVkIjoidGVzdCIsInRyYW5zYWN0aW9uX2lkZW50aWZpZXIiOiJ0ZXN0IiwiaWRlbnRpdHlfYXNzdXJhbmNlX2xldmVsIjozLCJmYW1pbHlfbmFtZSI6IlRIUkVFIiwiaWF0IjoxNTgzNDMyMzI2LCJqdGkiOiJ0ZXN0IiwiZXhwIjoxNTgzNDM2ODgzfQ.X7SZI94VheWf_-94C4Up0s6EBrgfeC225CAxTvLAYyY\n";

	private final String jsonUserErrorResp = "{}";

	@InjectMocks
	OauthServicesImpl oauthServices;

	@Mock
	OauthProperties oauthProperties;

	@Mock
	JWTValidationServiceImpl tokenServices;

	public static MockWebServer mockBackEnd;

	@BeforeAll
	static void setUp() throws IOException {
		mockBackEnd = new MockWebServer();
		mockBackEnd.start();
	}

	@AfterAll
	static void tearDown() throws IOException {
		mockBackEnd.shutdown();
	}

	@BeforeEach
	void initialize() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		MockitoAnnotations.initMocks(this);
		String baseUrl = String.format("http://localhost:%s", mockBackEnd.getPort());
		Mockito.when(oauthProperties.getIdp()).thenReturn(baseUrl);
		Mockito.when(oauthProperties.getClientId()).thenReturn("123");
		Mockito.when(oauthProperties.getAuthorizePath()).thenReturn("/test");
		Mockito.when(oauthProperties.getTokenPath()).thenReturn("/test");
		Mockito.when(oauthProperties.getUserinfoPath()).thenReturn("/test");
	}

	@DisplayName("Error - getIDPRedirect oauth service")
	@Test
	void testIdpRedirectDefault() throws URISyntaxException {
		Assertions.assertThrows(Exception.class, () -> {
			oauthServices.getIDPRedirect("TEST");
		});
	}

	@DisplayName("Success - getToken oauth service default")
	@Test
	void testDefaultGetTokenSuccess() throws OauthServiceException {
		MockResponse mockResponse = new MockResponse();
		mockResponse.setBody(jsonTokenSuccessResp);
		mockResponse.addHeader("content-type: application/json;");
		mockResponse.setResponseCode(200);
		mockBackEnd.enqueue(mockResponse);
		AccessTokenResponse response = oauthServices.getToken("test", null);
		Assertions.assertEquals("abc.abc.abc", response.getTokens().getBearerAccessToken().getValue());
	}

	@DisplayName("Success - getToken oauth service other")
	@Test
	void testOtherGetTokenSuccess() throws OauthServiceException {
		MockResponse mockResponse = new MockResponse();
		mockResponse.setBody(jsonTokenSuccessResp);
		mockResponse.addHeader("content-type: application/json;");
		mockResponse.setResponseCode(200);
		mockBackEnd.enqueue(mockResponse);
		AccessTokenResponse response = oauthServices.getToken("test", "TEST");
		Assertions.assertEquals("abc.abc.abc", response.getTokens().getBearerAccessToken().getValue());
	}

	@DisplayName("Failure - getToken oauth service")
	@Test
	void testGetTokenFailure1() throws OauthServiceException {
		MockResponse mockResponse = new MockResponse();
		mockResponse.setBody(jsonTokenErrorResp);
		mockResponse.addHeader("content-type: application/json;");
		mockResponse.setResponseCode(200);
		mockBackEnd.enqueue(mockResponse);
		Assertions.assertThrows(OauthServiceException.class, () -> {
			oauthServices.getToken("test", null);
		});
	}

	@DisplayName("Failure - getToken oauth service")
	@Test
	void testGetTokenFailure2() throws OauthServiceException, IOException {
		tearDown();
		Assertions.assertThrows(OauthServiceException.class, () -> {
			oauthServices.getToken("test", null);
		});
		setUp();
	}

	@DisplayName("Failure - getToken oauth service")
	@Test
	void testGetTokenFailure3() throws OauthServiceException {
		MockResponse mockResponse = new MockResponse();
		mockResponse.setBody(jsonTokenErrorResp);
		mockResponse.addHeader("content-type: application/json;");
		mockResponse.setResponseCode(400);
		mockBackEnd.enqueue(mockResponse);
		Assertions.assertThrows(OauthServiceException.class, () -> {
			oauthServices.getToken("test", null);
		});
	}

	@DisplayName("Success - getUserInfo oauth service")
	@Test
	void testGetUserInfoSuccess() throws OauthServiceException {
		MockResponse mockResponse = new MockResponse();
		mockResponse.setBody(jwtSuccessResp);
		mockResponse.addHeader("content-type: application/jwt;charset=ISO-8859-1");
		mockResponse.setResponseCode(200);
		mockBackEnd.enqueue(mockResponse);
		JSONObject response = oauthServices.getUserInfo(new BearerAccessToken());
		Assertions.assertEquals("test", response.get("sub"));
	}

	@DisplayName("Failure - getUserInfo oauth service")
	@Test
	void testGetUserInfoFailure1() throws OauthServiceException {
		MockResponse mockResponse = new MockResponse();
		mockResponse.setBody(jsonUserErrorResp);
		mockResponse.addHeader("content-type: application/json;");
		mockResponse.setResponseCode(200);
		mockBackEnd.enqueue(mockResponse);
		Assertions.assertThrows(OauthServiceException.class, () -> {
			oauthServices.getUserInfo(new BearerAccessToken());
		});
	}

	@DisplayName("Failure - getUserInfo oauth service")
	@Test
	void testGetUserInfoFailure2() throws OauthServiceException, IOException {
		tearDown();
		Assertions.assertThrows(OauthServiceException.class, () -> {
			oauthServices.getUserInfo(new BearerAccessToken());
		});
		setUp();
	}

	@DisplayName("Failure - getUserInfo oauth service")
	@Test
	void testGetUserInfoFailure3() throws OauthServiceException {
		MockResponse mockResponse = new MockResponse();
		mockResponse.setBody(jsonUserErrorResp);
		mockResponse.addHeader("content-type: application/json;");
		mockResponse.setResponseCode(400);
		mockBackEnd.enqueue(mockResponse);
		Assertions.assertThrows(OauthServiceException.class, () -> {
			oauthServices.getUserInfo(new BearerAccessToken());
		});
	}

	@DisplayName("Success - login oauth service default")
	@Test
	void testDefaultLoginSuccess() throws OauthServiceException, URISyntaxException {
		MockResponse mockResponse = new MockResponse();
		mockResponse.setBody(jsonTokenSuccessResp);
		mockResponse.addHeader("content-type: application/json;");
		mockResponse.setResponseCode(200);
		mockBackEnd.enqueue(mockResponse);
		MockResponse mockResponse2 = new MockResponse();
		mockResponse2.setBody(jwtSuccessResp);
		mockResponse2.addHeader("content-type: application/jwt;charset=ISO-8859-1");
		mockResponse2.setResponseCode(200);
		mockBackEnd.enqueue(mockResponse2);
		when(tokenServices.validateBCSCIDToken(any())).thenReturn(new ValidationResponse(true, "success"));
		ResponseEntity<String> response = oauthServices.login("test", null);
		Assertions.assertNotNull(response);
	}

	@DisplayName("Success - login oauth service other")
	@Test
	void testOtherLoginSuccess() throws OauthServiceException, URISyntaxException {
		MockResponse mockResponse = new MockResponse();
		mockResponse.setBody(jsonTokenSuccessResp);
		mockResponse.addHeader("content-type: application/json;");
		mockResponse.setResponseCode(200);
		mockBackEnd.enqueue(mockResponse);
		MockResponse mockResponse2 = new MockResponse();
		mockResponse2.setBody(jwtSuccessResp);
		mockResponse2.addHeader("content-type: application/jwt;charset=ISO-8859-1");
		mockResponse2.setResponseCode(200);
		mockBackEnd.enqueue(mockResponse2);
		when(tokenServices.validateBCSCIDToken(any())).thenReturn(new ValidationResponse(true, "success"));
		ResponseEntity<String> response = oauthServices.login("test", "TEST");
		Assertions.assertNotNull(response);
	}

	@DisplayName("Error - login oauth service")
	@Test
	void testLoginError1() throws OauthServiceException, URISyntaxException {
		MockResponse mockResponse = new MockResponse();
		mockResponse.setBody(jsonTokenErrorResp);
		mockResponse.addHeader("content-type: application/json;");
		mockResponse.setResponseCode(200);
		mockBackEnd.enqueue(mockResponse);
		MockResponse mockResponse2 = new MockResponse();
		mockResponse2.setBody(jwtSuccessResp);
		mockResponse2.addHeader("content-type: application/jwt;charset=ISO-8859-1");
		mockResponse2.setResponseCode(200);
		mockBackEnd.enqueue(mockResponse2);
		when(tokenServices.validateBCSCIDToken(any())).thenReturn(new ValidationResponse(true, "success"));
		ResponseEntity<String> response = oauthServices.login(null, null);
		Assertions.assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
	}

	@DisplayName("Error - login oauth service (invalid token)")
	@Test
	void testLoginError2() throws OauthServiceException, URISyntaxException {
		MockResponse mockResponse = new MockResponse();
		mockResponse.setBody(jsonTokenSuccessResp);
		mockResponse.addHeader("content-type: application/json;");
		mockResponse.setResponseCode(200);
		mockBackEnd.enqueue(mockResponse);
		MockResponse mockResponse2 = new MockResponse();
		mockResponse2.setBody(jwtSuccessResp);
		mockResponse2.addHeader("content-type: application/jwt;charset=ISO-8859-1");
		mockResponse2.setResponseCode(200);
		mockBackEnd.enqueue(mockResponse2);
		when(tokenServices.validateBCSCIDToken(any())).thenReturn(new ValidationResponse(false, "failure"));
		ResponseEntity<String> response = oauthServices.login("code", null);
		Assertions.assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
	}
}
