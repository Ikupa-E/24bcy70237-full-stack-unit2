package com.example.postscheduler.security;

import com.example.postscheduler.exception.ErrorCode;
import com.example.postscheduler.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Security failures happen inside the filter chain, before any controller runs, so the
 * @RestControllerAdvice cannot see them. This class writes them in the same ErrorResponse format.
 *  - 401: no token, or a bad / expired token (AuthenticationEntryPoint)
 *  - 403: logged in, but the role is not allowed for this URL (AccessDeniedHandler)
 */
@Component
public class RestSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(RestSecurityErrorHandler.class);

    private final ObjectMapper objectMapper;

    public RestSecurityErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        Object reason = request.getAttribute(JwtAuthenticationFilter.ERROR_ATTRIBUTE);
        String message = (reason != null)
                ? reason.toString()
                : "Authentication required. Send a valid Bearer token.";
        log.warn("401 Unauthorized on {} {}: {}", request.getMethod(), request.getRequestURI(), message);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        write(request, response, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, message);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        log.warn("403 Forbidden on {} {} for user '{}'", request.getMethod(), request.getRequestURI(),
                MDC.get(JwtAuthenticationFilter.MDC_USER));
        write(request, response, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN,
                "You do not have permission to access this resource");
    }

    private void write(HttpServletRequest request, HttpServletResponse response,
                       HttpStatus status, ErrorCode code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                ErrorResponse.of(status, code, message, request.getRequestURI(), null));
    }
}
