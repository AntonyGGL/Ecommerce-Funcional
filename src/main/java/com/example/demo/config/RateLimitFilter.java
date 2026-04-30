package com.example.demo.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

// Filtro de rate limiting para endpoints de autenticacion.
// Limites: login=10/min, register/forgot/reset=5/min por IP.
// Responde 429 Too Many Requests cuando se supera el limite.
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    // Caches de buckets por IP (para alta escala usar bucket4j-redis)
    private final ConcurrentHashMap<String, Bucket> loginBuckets       = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> sensitiveOpBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path   = request.getRequestURI();
        String method = request.getMethod();

        if (isLoginPath(method, path)) {
            String ip = resolveClientIp(request);
            Bucket bucket = loginBuckets.computeIfAbsent(ip, k -> buildLoginBucket());
            consumeOrReject(bucket, response, filterChain, request, ip, path);
            return;
        }

        if (isSensitivePath(method, path)) {
            String ip = resolveClientIp(request);
            Bucket bucket = sensitiveOpBuckets.computeIfAbsent(ip, k -> buildSensitiveBucket());
            consumeOrReject(bucket, response, filterChain, request, ip, path);
            return;
        }

        filterChain.doFilter(request, response);
    }

    // ------------------------------------------------------------------ helpers

    private void consumeOrReject(Bucket bucket,
                                  HttpServletResponse response,
                                  FilterChain chain,
                                  HttpServletRequest request,
                                  String ip,
                                  String path) throws ServletException, IOException {

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            log.warn("Rate limit superado | IP: {} path: {}", ip, path);
            response.addHeader("Retry-After", "60");
            response.addHeader("X-Rate-Limit-Remaining", "0");
            response.setContentType("application/json");
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write(
                "{\"error\":\"Demasiadas solicitudes. Intenta de nuevo en 60 segundos.\"}"
            );
        }
    }

    // 10 solicitudes por minuto para login
    private Bucket buildLoginBucket() {
        Bandwidth limit = Bandwidth.builder()
            .capacity(10)
            .refillGreedy(10, Duration.ofMinutes(1))
            .build();
        return Bucket.builder().addLimit(limit).build();
    }

    // 5 solicitudes por minuto para operaciones sensibles
    private Bucket buildSensitiveBucket() {
        Bandwidth limit = Bandwidth.builder()
            .capacity(5)
            .refillGreedy(5, Duration.ofMinutes(1))
            .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private boolean isLoginPath(String method, String path) {
        return "POST".equalsIgnoreCase(method) && path.equals("/api/auth/login");
    }

    private boolean isSensitivePath(String method, String path) {
        return "POST".equalsIgnoreCase(method)
            && (path.equals("/api/auth/register")
                || path.equals("/api/auth/forgot-password")
                || path.equals("/api/auth/reset-password"));
    }

    // Resuelve la IP real del cliente considerando proxies y load-balancers
    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Solo tomar la primera IP de la cadena (la del cliente original)
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }
}
