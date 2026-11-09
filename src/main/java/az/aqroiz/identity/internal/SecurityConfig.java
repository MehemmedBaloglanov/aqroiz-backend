package az.aqroiz.identity.internal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import az.aqroiz.shared.security.JwtClaims;

/**
 * Tətbiqin təhlükəsizlik qaydaları.
 * <p>
 * - Stateless: sessiya yoxdur, hər sorğu JWT ilə özünü tanıdır.
 * - CSRF söndürülüb: token cookie-də yox, Authorization başlığındadır; brauzer onu özü əlavə etmir.
 *   Token gələcəkdə cookie-yə köçürülsə, CSRF qorunması MÜTLƏQ geri qaytarılmalıdır.
 * - Standart olaraq hər şey bağlıdır; açıq endpoint-lər aşağıda açıq şəkildə sadalanır.
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ProblemDetailSecurityHandler problemHandler)
            throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Monitorinq (Docker, server yoxlamaları)
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        // Giriş
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        // Spring-in daxili xəta səhifəsi: bağlı olsa, əsl xəta 401 ilə gizlənərdi
                        .requestMatchers("/error").permitAll()
                        // Qalan hər şey
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(problemHandler)
                        .accessDeniedHandler(problemHandler))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(problemHandler)
                        .accessDeniedHandler(problemHandler));
        return http.build();
    }

    /**
     * JWT-dəki "role" claim-ini Spring icazəsinə çevirir: "MANAGER" → "ROLE_MANAGER".
     * Bu sayədə @PreAuthorize("hasRole('MANAGER')") işləyir.
     */
    static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(JwtClaims.ROLE);
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
