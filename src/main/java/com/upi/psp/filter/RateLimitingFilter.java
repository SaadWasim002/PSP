package com.upi.psp.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 5;
    private static final long WINDOW_MS = 60_000L;
    private final ConcurrentHashMap<String, List<Long>> requestLogs = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Apply only to POST /psp/api/pay
        if ("POST".equalsIgnoreCase(method) && ("/psp/api/pay".equals(path) || "/psp/api/pay/".equals(path))) {
            String clientIp = getClientIp(request);
            long currentTime = System.currentTimeMillis();

            List<Long> timestamps = requestLogs.compute(clientIp, (key, value) -> {
                if (value == null) {
                    value = Collections.synchronizedList(new ArrayList<>());
                }
                // Evict timestamps older than 60 seconds
                value.removeIf(time -> time < currentTime - WINDOW_MS);
                return value;
            });

            synchronized (timestamps) {
                if (timestamps.size() >= MAX_REQUESTS) {
                    // Rate limit exceeded
                    long oldestRequestTime = timestamps.get(0);
                    long retryAfterMs = (oldestRequestTime + WINDOW_MS) - currentTime;

                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");

                    Map<String, Object> errorDetails = new HashMap<>();
                    errorDetails.put("error_code", "RATE_LIMIT_EXCEEDED");
                    errorDetails.put("message", "Rate limit exceeded. Too many requests.");
                    errorDetails.put("transaction_id", null);
                    errorDetails.put("retry_after_ms", retryAfterMs > 0 ? retryAfterMs : 0);

                    response.getWriter().write(objectMapper.writeValueAsString(errorDetails));
                    return; // Short-circuit
                } else {
                    timestamps.add(currentTime);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isBlank()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
