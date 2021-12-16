package ca.bc.gov.open.oauth.security;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import ca.bc.gov.open.oauth.configuration.OauthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

public class JWTAuthorizationFilter  extends OncePerRequestFilter {
	
	private final Logger jwtLogger = LoggerFactory.getLogger(JWTAuthorizationFilter.class);

    private OauthProperties oauthProperties;

    public JWTAuthorizationFilter(OauthProperties oauthProperties) {
        this.oauthProperties = oauthProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        try {
            jwtLogger.info(request.getContextPath());
            if (request.getContextPath().contains("/oauth/actuator/health")) {
                chain.doFilter(request, response);
                return;
            }
			if (checkJWTToken(request)) {
				jwtLogger.debug("JWT found in header.");
				Claims claims = validateToken(request);
				if (claims.get("clientId") != null) {
					oauthProperties.setClientId(claims.get("clientId").toString());
					jwtLogger.debug("JWT passed basic validation checks.");
					UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
							claims.getSubject(), null, null);
					SecurityContextHolder.getContext().setAuthentication(auth);
				} else {
					throw new AuthenticationCredentialsNotFoundException("Client Id not found");
				}
			} else {
                throw new AuthenticationCredentialsNotFoundException("Jwt not found");
			}
			chain.doFilter(request, response);
		} catch (Exception e) {
            jwtLogger.info("Authentication failed: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            (response).sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        }
    }

    private Claims validateToken(HttpServletRequest request) {
        String jwtToken = request.getHeader(oauthProperties.getJwtHeader()).replace(oauthProperties.getJwtPrefix(), "").trim();
        return Jwts.parser().setSigningKey(oauthProperties.getJwtSecret().getBytes()).parseClaimsJws(jwtToken).getBody();
    }

    private boolean checkJWTToken(HttpServletRequest request) {
        String authenticationHeader = request.getHeader(oauthProperties.getJwtHeader());
        return !(authenticationHeader == null || !authenticationHeader.startsWith(oauthProperties.getJwtPrefix()));
    }
}
