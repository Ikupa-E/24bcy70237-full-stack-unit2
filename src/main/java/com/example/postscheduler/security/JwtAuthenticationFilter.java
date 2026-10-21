package com.example.postscheduler.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Runs once per request, before the authorization checks. If the request carries
 * "Authorization: Bearer <token>", the token is verified and the user is placed in the
 * SecurityContext. Nothing is stored on the server between requests (stateless).
 * If the token is bad, the request simply stays unauthenticated and is rejected later with a 401.
 *
 * Note: this class is deliberately NOT a Spring bean, so it is only registered inside the
 * security filter chain (see SecurityConfig) and not a second time by the servlet container.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String ERROR_ATTRIBUTE = "jwt.error";
    public static final String MDC_USER = "user";

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        try {
            if (header != null && header.startsWith(BEARER_PREFIX)) {
                authenticate(request, header.substring(BEARER_PREFIX.length()).trim());
            }
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_USER);
        }
    }

    private void authenticate(HttpServletRequest request, String token) {
        try {
            Claims claims = jwtService.parse(token);
            String username = claims.getSubject();
            String role = claims.get("role", String.class);

            if (username != null && role != null
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                MDC.put(MDC_USER, username);   // shows up in every log line of this request
            }
        } catch (ExpiredJwtException ex) {
            request.setAttribute(ERROR_ATTRIBUTE, "Token has expired. Please log in again.");
            log.warn("Rejected expired JWT");
        } catch (JwtException | IllegalArgumentException ex) {
            request.setAttribute(ERROR_ATTRIBUTE, "Invalid token");
            log.warn("Rejected invalid JWT ({})", ex.getClass().getSimpleName());
        }
    }
}
