package ca.bc.gov.open.oauth.security;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.context.SecurityContextHolder;

import ca.bc.gov.open.oauth.configuration.OauthProperties;
import ca.bc.gov.open.oauth.service.JWTValidationServiceImpl;

/**
 * Tests for jwt authorization filter
 * 
 * @author sivakaruna
 *
 */
class JWTAuthorizationFilterTest {

	private final String jwtSuccess = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
			+ "eyJhdXRob3JpdGllcyI6WyJST0xFIl0sImNsaWVudElkIjoidGVzdCIsImlhdCI6MTU4ODYzMDM5MCwiZXhwIjo5OTk5OTk5OTk5fQ."
			+ "RB_s96R_bW_GkhFPBcERC2AEd0I5JKeN8g6X3NYpOME";
	private final String jwtRunTimeError = "eyJhbGciOiJIUzI1NiJ9."
			+ "eyJoZWFkZXIiOiJwcmVmaXgiLCJhdXRob3JpdGllcyI6InJvbGUifQ." + "Xyh5YRGNLdaPfxCUak6dokgoWb9EA51w9LslqcndWjU";
	private final String jwtSignError = "eyJhbGciOiJIUzI1NiJ9."
			+ "eyJoZWFkZXIiOiJwcmVmaXgifQ.wAiFql12MJHfbQEl10q-5PlIgBCKprhR-PAUpec5XNo";

	@InjectMocks
	JWTAuthorizationFilter filter;

	@Mock
	OauthProperties oauthProps;

	@Mock
	JWTValidationServiceImpl tokenValidationServices;

	@BeforeEach
	void initialize() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		MockitoAnnotations.initMocks(this);
		Mockito.when(oauthProps.getJwtHeader()).thenReturn("header");
		Mockito.when(oauthProps.getJwtPrefix()).thenReturn("prefix");
		Mockito.when(oauthProps.getJwtSecret()).thenReturn("secret");
	}

	@DisplayName("Success - doFilterInternal jwt filter")
	@Test
	void testSuccess() throws ServletException, IOException {
		SecurityContextHolder.clearContext();
		// Valid JWT
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(request) {
			@Override
			public String getHeader(String name) {
				return "prefix" + jwtSuccess;
			}
			@Override
			public String getRequestURI() { return "test"; }
		};
		HttpServletResponse response = mock(HttpServletResponse.class);
		FilterChain chain = mock(FilterChain.class);
		filter.doFilterInternal(wrapper, response, chain);
		Assertions.assertTrue(SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
	}

	@DisplayName("Missing Header - doFilterInternal jwt filter")
	@Test
	void testMissingHeader() throws ServletException, IOException {
		SecurityContextHolder.clearContext();
		// Missing header
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		FilterChain chain = mock(FilterChain.class);
		filter.doFilterInternal(request, response, chain);
		Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
	}

	@DisplayName("Invalid Signature - doFilterInternal jwt filter")
	@Test
	void testInvalidSignature() throws ServletException, IOException {
		SecurityContextHolder.clearContext();
		// Invalid signature
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(request) {

			@Override
			public String getHeader(String name) {
				return "prefix" + jwtSignError;
			}
		};
		HttpServletResponse response = mock(HttpServletResponse.class);
		FilterChain chain = mock(FilterChain.class);
		filter.doFilterInternal(wrapper, response, chain);
		Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
	}

	@DisplayName("Error - doFilterInternal jwt filter")
	@Test
	void testError() throws ServletException, IOException {
		SecurityContextHolder.clearContext();
		// Authority list malformed
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(request) {

			@Override
			public String getHeader(String name) {
				return "prefix" + jwtRunTimeError;
			}
		};
		HttpServletResponse response = mock(HttpServletResponse.class);
		FilterChain chain = mock(FilterChain.class);
		filter.doFilterInternal(wrapper, response, chain);
		Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
	}
}
