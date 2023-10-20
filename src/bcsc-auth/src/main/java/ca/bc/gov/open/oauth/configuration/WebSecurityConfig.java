package ca.bc.gov.open.oauth.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import ca.bc.gov.open.oauth.security.JWTAuthorizationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@EnableWebSecurity
@Configuration
public class WebSecurityConfig {

	@Autowired
	OauthProperties oauthProps;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		return http.csrf(csrf -> csrf.disable())
				.addFilterAfter(new JWTAuthorizationFilter(oauthProps), UsernamePasswordAuthenticationFilter.class)
				.authorizeHttpRequests(authorizeRequests ->
						authorizeRequests
								.requestMatchers(new AntPathRequestMatcher("/actuator/**")).permitAll()
								.anyRequest()
								.authenticated()
				)
				.httpBasic(Customizer.withDefaults())
				.build();
	}

	@Bean
	public InMemoryUserDetailsManager userDetailsService() {
		UserDetails user =
				User.builder()
						.username(oauthProps.getUsername())
						.password(passwordEncoder().encode(oauthProps.getPassword()))
						.roles("USER")
						.build();
		return new InMemoryUserDetailsManager(user);
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
