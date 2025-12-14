package com.kuru.delivery.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuru.delivery.auth.service.CustomUserDetailsService;
import com.kuru.delivery.auth.service.TokenBlacklistService;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;
    private final TokenBlacklistService tokenBlacklistService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtAuthenticationFilter(JwtUtil jwtUtil, 
                                   CustomUserDetailsService customUserDetailsService,
                                   TokenBlacklistService tokenBlacklistService) {
        this.jwtUtil = jwtUtil;
        this.customUserDetailsService = customUserDetailsService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            final String token = authHeader.substring(7).trim();

            if (tokenBlacklistService.isTokenBlacklisted(token)) {
                writeUnauthorizedJson(response, request.getServletPath(), "Token has been revoked");
                return;
            }

            try {
                String username = jwtUtil.extractUsername(token);
                String tokenRole = jwtUtil.extractRole(token);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    try {
                        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

                        // CRITICAL SECURITY: Verify token role matches database role
                        String dbRole = userDetails.getAuthorities().stream()
                                .findFirst()
                                .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                                .orElse(null);

                        if (dbRole == null || !dbRole.equals(tokenRole)) {
                            logger.warn("Role mismatch detected for user: " + username + 
                                      ". Token role: " + tokenRole + ", DB role: " + dbRole);
                            writeUnauthorizedJson(response, request.getServletPath(), 
                                "Token role does not match user role. Please login again.");
                            return;
                        }

                        if (jwtUtil.validateToken(token, userDetails.getUsername())) {
                            UsernamePasswordAuthenticationToken authToken =
                                    new UsernamePasswordAuthenticationToken(
                                            userDetails,
                                            null,
                                            userDetails.getAuthorities()
                                    );
                            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authToken);
                        }
                    } catch (Exception e) {
                        // User not found or other error loading user - ignore token and proceed as anonymous
                        logger.warn("Could not set user authentication in security context", e);
                    }
                }

            } catch (ExpiredJwtException eje) {
                writeUnauthorizedJson(response, request.getServletPath(), "Token expired");
                return; 
            } catch (JwtException | IllegalArgumentException ex) {
                writeUnauthorizedJson(response, request.getServletPath(), "Invalid token");
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        if (path == null) return false;
        if (path.equals("/api/auth/debug")) return false;
        if (path.equals("/api/auth/me")) return false;
        if (path.equals("/api/auth/logout")) return false;
        
        return path.startsWith("/api/auth/")
                || path.startsWith("/h2-console/")
                || path.startsWith("/swagger-ui/")
                || path.startsWith("/v3/api-docs");
    }

    private void writeUnauthorizedJson(HttpServletResponse response, String path, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        Map<String, Object> body = new HashMap<>();
        body.put("status", 401);
        body.put("error", "Unauthorized");
        body.put("message", message);
        body.put("path", path);

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
