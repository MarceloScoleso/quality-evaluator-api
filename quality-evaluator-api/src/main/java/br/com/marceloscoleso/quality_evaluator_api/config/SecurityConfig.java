package br.com.marceloscoleso.quality_evaluator_api.config;
 
import br.com.marceloscoleso.quality_evaluator_api.security.JwtAuthenticationFilter;
 
import java.util.List;
 
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
 
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
 
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
 
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }
 
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
 
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // ── rotas públicas ──────────────────────────────────────────
                .requestMatchers(
                    "/auth/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()
 
                // ── monitoramento: apenas ADMIN ─────────────────────────────
                .requestMatchers("/health", "/info", "/metrics").hasRole("ADMIN")
 
                // ── GitHub: apenas usuários autenticados ────────────────────
                // (o JwtAuthenticationFilter já valida o Bearer token)
                .requestMatchers("/api/github/**").authenticated()
 
                // ── todo o resto também exige autenticação ──────────────────
                .anyRequest().authenticated()
            )
            .headers(headers -> headers
                // ✅ frameOptions seguro (sameOrigin em vez de disable total)
                .frameOptions(frame -> frame.sameOrigin())
                // ✅ headers de segurança adicionais
                .contentSecurityPolicy(csp ->
                    csp.policyDirectives("default-src 'self'")
                )
                .referrerPolicy(ref ->
                    ref.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
            );
 
        http.addFilterBefore(
            jwtAuthenticationFilter,
            UsernamePasswordAuthenticationFilter.class
        );
 
        return http.build();
    }
 
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
 
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
 
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
 
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(List.of(
            "https://code-quality-evaluator-frontend-59azxcgth.vercel.app",
            "https://code-quality-evaluator-frontend.vercel.app",
            // durante desenvolvimento local
            "http://localhost:3000"
        ));
 
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setMaxAge(3600L); // cache preflight por 1h
 
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}