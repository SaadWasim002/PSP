package com.upi.psp.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, 1024 * 1024);

        try {
            filterChain.doFilter(wrappedRequest, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            String method = wrappedRequest.getMethod();
            String path = wrappedRequest.getRequestURI();
            int status = response.getStatus();

            String maskedBody = getMaskedBody(wrappedRequest);

            if (maskedBody != null && !maskedBody.isBlank()) {
                log.info("Request: {} {} | Status: {} | Duration: {}ms | Body: {}", method, path, status, duration, maskedBody);
            } else {
                log.info("Request: {} {} | Status: {} | Duration: {}ms", method, path, status, duration);
            }
        }
    }

    private String getMaskedBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length == 0) {
            return "";
        }

        String body = new String(content, StandardCharsets.UTF_8);
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.isObject()) {
                ObjectNode objectNode = (ObjectNode) root;
                // Mask the upi_pin field if present
                if (objectNode.has("upi_pin")) {
                    objectNode.put("upi_pin", "******");
                }
                // Mask the upi_pin_hash field just in case
                if (objectNode.has("upi_pin_hash")) {
                    objectNode.put("upi_pin_hash", "******");
                }
                return objectMapper.writeValueAsString(objectNode);
            }
            return body;
        } catch (Exception e) {
            // Fallback to regex masking if parsing fails
            return maskPinWithRegex(body);
        }
    }

    private String maskPinWithRegex(String body) {
        if (body == null) return "";
        // Regex replacement for "upi_pin": "..."
        return body.replaceAll("\"upi_pin\"\\s*:\\s*\"[^\"]*\"", "\"upi_pin\":\"******\"")
                   .replaceAll("\"upi_pin_hash\"\\s*:\\s*\"[^\"]*\"", "\"upi_pin_hash\":\"******\"");
    }
}
