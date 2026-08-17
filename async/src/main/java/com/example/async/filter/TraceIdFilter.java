package com.example.async.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * <p>
 * Puts a trace id in the MDC for the duration of the request. It only lives on the request thread,
 * which is exactly why the task decorator in AsyncConfig has to copy it over to the worker thread;
 * the {@code traceId} field of a TaskResult shows whether that copy worked.
 * </p>
 *
 * @author NamHoang
 */
@Component
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        MDC.put(TRACE_ID, UUID.randomUUID().toString().substring(0, 8));
        try {
            chain.doFilter(request, response);
        } finally {
            // The request thread goes back to the pool, so the id must not stay behind on it.
            MDC.remove(TRACE_ID);
        }
    }
}
