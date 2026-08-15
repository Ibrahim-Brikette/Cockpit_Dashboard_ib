package com.dynamicdashboard.cockpit.shared.security;
import java.util.List;

import com.dynamicdashboard.cockpit.shared.security.jwt.AudienceValidator;
import com.dynamicdashboard.cockpit.shared.security.jwt.JwtAbstractAuthoritiesConverter;
import com.dynamicdashboard.cockpit.shared.security.jwt.JwtAuthoritiesConverter;
import com.dynamicdashboard.cockpit.shared.security.permission_evaluators.DelegatingPermissionEvaluator;
import com.dynamicdashboard.cockpit.shared.security.permission_evaluators.DomainPermissionEvaluator;
// Old: individual imports needed when each evaluator was injected as a named field.
// import com.dynamicdashboard.cockpit.shared.security.permission_evaluators.DataQueryPermissionEvaluator;
// import com.dynamicdashboard.cockpit.shared.security.permission_evaluators.DashboardQueryPermissionEvaluator;
import com.dynamicdashboard.cockpit.shared.security.tenant.TenantContextFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
@Slf4j
@RequiredArgsConstructor
public class SecurityConfiguration {
    private final JwtAuthoritiesConverter jwtAuthoritiesConverter;
    private final TenantContextFilter tenantContextFilter;
    // Old: individual fields — adding a new domain evaluator meant adding a new field here too.
    // private final DataQueryPermissionEvaluator dataQueryPermissionEvaluator;
    // private final DashboardQueryPermissionEvaluator dashboardQueryPermissionEvaluator;
    private final List<DomainPermissionEvaluator> domainPermissionEvaluators;
//    It scans the application context, finds every
//    @Component that implements DomainPermissionEvaluator,
//    and injects them all into that list automatically. No registration code anywhere.
    @Value("${app.security.enabled:true}")
    private boolean securityEnabled;
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .contentSecurityPolicy(
                                csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none';"))
                        .xssProtection(Customizer.withDefaults())
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(
                                referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000)))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/actuator/health", "/actuator/info").permitAll()
                            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    if (securityEnabled) {
                        auth.requestMatchers("/api/auth/login", "/api/auth/refresh").permitAll()
                                .anyRequest().authenticated();
                    } else {
                        auth.anyRequest().permitAll();
                    }
                })
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthoritiesConverter))
                )
                .addFilterAfter(tenantContextFilter, org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter.class);

        return http.build();
    }
    @Bean
    CorsConfigurationSource corsConfigurationSource(SecurityProperties securityProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-Id"));
        configuration.setExposedHeaders(List.of("X-Request-Id"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        var handler = new DefaultMethodSecurityExpressionHandler();
        // Old (v1): single evaluator — DashboardQueryPermissionEvaluator was never reached,
        // every hasPermission(..., 'Dashboard', ...) silently returned false.
        // handler.setPermissionEvaluator(dataQueryPermissionEvaluator);
        //
        // Old (v2): delegating but hardcoded map — adding a new domain required changing this class.
        // handler.setPermissionEvaluator(new DelegatingPermissionEvaluator(Map.of(
        //         "Dashboard", dashboardQueryPermissionEvaluator,
        //         "Query", dataQueryPermissionEvaluator
        // )));
        handler.setPermissionEvaluator(new DelegatingPermissionEvaluator(domainPermissionEvaluators));
        return handler;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
//    @Bean
//    JwtAbstractAuthenticationConverter jwtAuthenticationConverter() {
//        JwtAbstractAuthenticationConverter converter = new JwtAbstractAuthenticationConverter();
//        converter.setJwtGrantedAuthoritiesConverter(new JwtAbstractAuthoritiesConverter());
//        return converter;
//    }
//    @Bean
//    JwtDecoder jwtDecoder(SecurityProperties securityProperties,
//            org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties properties) {
//        try {
//            NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(properties.getJwt().getIssuerUri()).build();
//            OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(securityProperties.getRequiredAudience());
//            OAuth2TokenValidator<Jwt> defaultValidators = JwtValidators
//                    .createDefaultWithIssuer(properties.getJwt().getIssuerUri());
//            decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaultValidators, audienceValidator));
//            return decoder;
//        } catch (Exception e) {
//            return token -> null;
//        }
//    }
}
