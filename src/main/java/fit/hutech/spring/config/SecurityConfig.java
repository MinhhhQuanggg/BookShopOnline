package fit.hutech.spring.config;

import fit.hutech.spring.services.OauthService;
import fit.hutech.spring.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.SecurityContextHolderFilter;

import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {
    private final UserService userService;
    private final OauthService oauthService;
    private final InvalidateSessionOnRestartFilter invalidateSessionOnRestartFilter;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> response.sendRedirect("/error/403");
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .addFilterBefore(invalidateSessionOnRestartFilter, SecurityContextHolderFilter.class)
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(
                                "/register",
                                "/login",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/error/**",
                                "/payments/**")
                        .permitAll()
                        .requestMatchers("/books", "/books/", "/books/list", "/home", "/").authenticated()
                        .requestMatchers("/cart/**", "/books/add-to-cart").authenticated()
                        .requestMatchers("/admin/**").hasAuthority("ADMIN")
                        .requestMatchers("/books/add", "/books/edit/**", "/books/edit", "/books/delete/**")
                        .hasAnyAuthority("ADMIN", "USER")
                        .requestMatchers("/books/**").hasAnyAuthority("USER", "ADMIN")
                        .requestMatchers("/api/**").hasAnyAuthority("ADMIN", "USER")
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler(accessDeniedHandler()))
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(authenticationSuccessHandler())
                        .failureUrl("/login?error=true")
                        .permitAll())
                .oauth2Login(oauth -> oauth
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo.userService(oauthService))
                        .successHandler(authenticationSuccessHandler()))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login")
                        .permitAll())
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (HttpServletRequest request, HttpServletResponse response, Authentication authentication) -> {
            if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
                OAuth2User oauthUser = oauthToken.getPrincipal();
                Map<String, Object> attributes = oauthUser.getAttributes();
                String email = (String) attributes.get("email");
                if (email != null && !email.isBlank()) {
                    String username = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
                    userService.saveOauthUser(email, username);
                }
            }
            response.sendRedirect("/home");
        };
    }
}
