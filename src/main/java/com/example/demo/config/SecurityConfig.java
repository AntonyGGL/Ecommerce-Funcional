package com.example.demo.config;

import com.example.demo.security.JwtTokenProvider;
import com.example.demo.security.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    // Origenes CORS permitidos - inyectados desde la variable de entorno ALLOWED_ORIGINS
    // Ejemplo prod: https://impofer.com,https://www.impofer.com
    @Value("${app.cors.allowed-origins:http://localhost:8080,http://localhost:3000}")
    private String[] allowedOrigins;

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        // Ignora completamente estas rutas en Spring Security (no aplica ningún filtro)
        return (web) -> web.ignoring()
            .requestMatchers(
                "/js/**", "/css/**", "/img/**", "/images/**", "/Imagen/**",
                "/*.css", "/*.js", "/*.ico", "/*.png", "/*.jpg",
                "/static/**", "/*.html"
            );
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt con coste 12 para produccion (equilibrio seguridad / rendimiento)
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Solo los origenes configurados; nunca wildcard en produccion
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF: deshabilitado correctamente para APIs REST stateless con JWT
            .csrf(csrf -> csrf.disable())

            // Sesiones stateless - el estado reside en el JWT
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Headers de seguridad HTTP
            .headers(headers -> headers
                // Previene clickjacking
                .frameOptions(frame -> frame.deny())
                // Fuerza HTTPS durante 1 año (incluye subdominios)
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                    .preload(true))
                // Evita MIME-type sniffing
                .contentTypeOptions(ct -> {})
                // Politica de referencia segura
                .referrerPolicy(ref -> ref
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                // Content Security Policy
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; " +
                    "script-src 'self' 'unsafe-inline' https://cdn.tailwindcss.com https://unpkg.com; " +
                    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                    "img-src 'self' data: https:; " +
                    "connect-src 'self' https://cdn.tailwindcss.com; " +
                    "font-src 'self' https://fonts.gstatic.com; " +
                    "form-action 'self'; " +
                    "base-uri 'self'"
                ))
                // Permisos del navegador (camara, mic, geolocacion desactivados)
                .addHeaderWriter(new StaticHeadersWriter(
                    "Permissions-Policy",
                    "camera=(), microphone=(), geolocation=()"
                ))
            )

            .authorizeHttpRequests(authorize -> authorize
                // Rutas publicas - Productos (solo lectura)
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()

                // Rutas publicas - Autenticacion
                .requestMatchers("/api/auth/**").permitAll()

                // Libro de Reclamaciones — cualquier visitante puede registrar (INDECOPI)
                .requestMatchers(HttpMethod.POST, "/api/reclamaciones").permitAll()

                // Pasarela de Pagos Culqi (cargo síncrono — verificación por lógica interna)
                .requestMatchers(HttpMethod.POST, "/api/payments/process-payment").permitAll()

                // Cleanup endpoint para desarrollo
                .requestMatchers(HttpMethod.DELETE, "/api/admin/cleanup/orders").permitAll()

                // Archivos estaticos - permitir todos los recursos estáticos con patrones de directorio
                .requestMatchers(
                    "/", "/index.html", "/login.html", "/catalog.html",
                    "/checkout.html", "/my-orders.html", "/reset-password.html",
                    "/clients-admin.html", "/inventory-admin.html", "/orders-admin.html", "/admin.html",
                    "/css/**", "/js/**", "/img/**", "/images/**", "/Imagen/**",
                    "/*.css", "/*.js", "/*.ico", "/*.png", "/*.jpg",
                    "/static/**"
                ).permitAll()

                // Rutas protegidas - solo ADMIN
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/clients/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/clients/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/clients/**").hasRole("ADMIN")

                // Rutas protegidas - usuarios autenticados
                .requestMatchers(HttpMethod.GET, "/api/orders/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/orders").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/orders/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/orders/*/confirm-delivery").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/orders/*/address").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/orders/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/orders/**").hasRole("ADMIN")

                // Todo lo demas requiere autenticacion
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                // Returns 401 JSON when token is missing or expired (instead of default 403)
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"error\":\"Sesión expirada. Por favor inicia sesión nuevamente.\"}");
                })
            )
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .addFilterBefore(new JwtTokenFilter(jwtTokenProvider, customUserDetailsService),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
