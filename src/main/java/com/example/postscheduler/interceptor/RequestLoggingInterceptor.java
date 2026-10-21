package com.example.postscheduler.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Logs each request when it arrives and a summary (status + duration) when it completes.
 * Request bodies are deliberately not logged, to avoid leaking sensitive data.
 */
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
    private static final String START_TIME = "requestStartTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME, System.currentTimeMillis());
        log.info("--> {} {} from {}", request.getMethod(), fullPath(request), request.getRemoteAddr());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Long start = (Long) request.getAttribute(START_TIME);
        long duration = (start == null) ? -1 : System.currentTimeMillis() - start;
        int status = response.getStatus();

        String line = "<-- {} {} status={} duration={}ms";
        if (status >= 500) {
            log.error(line, request.getMethod(), fullPath(request), status, duration);
        } else if (status >= 400) {
            log.warn(line, request.getMethod(), fullPath(request), status, duration);
        } else {
            log.info(line, request.getMethod(), fullPath(request), status, duration);
        }
    }

    private String fullPath(HttpServletRequest request) {
        String query = request.getQueryString();
        return (query == null) ? request.getRequestURI() : request.getRequestURI() + "?" + query;
    }
}
