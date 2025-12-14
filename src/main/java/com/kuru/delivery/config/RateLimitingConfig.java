package com.kuru.delivery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class RateLimitingConfig implements WebMvcConfigurer {

    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final long TIME_WINDOW_MS = 60_000;
    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();

    @Bean
    public RateLimitingInterceptor rateLimitingInterceptor() {
        return new RateLimitingInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitingInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/payments/webhook"
                );
    }

    private static class RequestCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long resetTime = System.currentTimeMillis() + TIME_WINDOW_MS;

        public int incrementAndGet() {
            long now = System.currentTimeMillis();
            if (now > resetTime) {
                count.set(0);
                resetTime = now + TIME_WINDOW_MS;
            }
            return count.incrementAndGet();
        }

        public boolean isLimitExceeded() {
            return count.get() > MAX_REQUESTS_PER_MINUTE;
        }
    }

    public class RateLimitingInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            String clientIp = getClientIpAddress(request);
            
            RequestCounter counter = requestCounts.computeIfAbsent(clientIp, k -> new RequestCounter());
            int currentCount = counter.incrementAndGet();

            if (counter.isLimitExceeded()) {
                response.setStatus(429);
                response.setContentType("application/json");
                try {
                    response.getWriter().write("{\"error\":\"Rate limit exceeded. Please try again later.\"}");
                } catch (Exception e) {
                }
                return false;
            }
            response.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS_PER_MINUTE));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, MAX_REQUESTS_PER_MINUTE - currentCount)));

            return true;
        }

        private String getClientIpAddress(HttpServletRequest request) {
            // SECURITY: Only trust proxy headers if behind a trusted proxy/load balancer
            boolean trustProxy = "true".equalsIgnoreCase(System.getenv("TRUST_PROXY"));
            
            if (trustProxy) {
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    // SECURITY: Validate IP format to prevent injection
                    String firstIp = xForwardedFor.split(",")[0].trim();
                    if (isValidIpAddress(firstIp)) {
                        return firstIp;
                    }
                }
                
                String xRealIp = request.getHeader("X-Real-IP");
                if (xRealIp != null && !xRealIp.isEmpty() && isValidIpAddress(xRealIp)) {
                    return xRealIp;
                }
            }
            
            return request.getRemoteAddr();
        }

        // SECURITY: Basic IP address validation to prevent injection attacks
        private boolean isValidIpAddress(String ip) {
            if (ip == null || ip.isEmpty()) {
                return false;
            }
            return ip.matches("^([0-9]{1,3}\\.){3}[0-9]{1,3}$") || 
                   ip.matches("^([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}$") ||
                   ip.equals("localhost") || ip.equals("127.0.0.1") || ip.equals("::1");
        }
    }
}

