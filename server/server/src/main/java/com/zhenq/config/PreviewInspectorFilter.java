package com.zhenq.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 为 /preview/** 下的 HTML 响应注入可视化选区脚本
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class PreviewInspectorFilter extends OncePerRequestFilter {

    private static final String INSPECTOR_SCRIPT =
            "<script src=\"/api/preview-inspector.js\" defer></script>";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (!"GET".equalsIgnoreCase(request.getMethod()) || uri == null || !uri.contains("/preview/")) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingResponseWrapper wrapped = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(request, wrapped);

        byte[] body = wrapped.getContentAsByteArray();
        String contentType = wrapped.getContentType();
        if (body.length == 0 || contentType == null || !contentType.contains("text/html")) {
            wrapped.copyBodyToResponse();
            return;
        }

        String html = new String(body, StandardCharsets.UTF_8);
        if (html.contains("preview-inspector.js")) {
            wrapped.copyBodyToResponse();
            return;
        }

        String injected = injectScript(html);
        byte[] out = injected.getBytes(StandardCharsets.UTF_8);
        response.setContentLength(out.length);
        response.getOutputStream().write(out);
    }

    private String injectScript(String html) {
        String lower = html.toLowerCase();
        int bodyClose = lower.lastIndexOf("</body>");
        if (bodyClose >= 0) {
            return html.substring(0, bodyClose) + INSPECTOR_SCRIPT + html.substring(bodyClose);
        }
        return html + INSPECTOR_SCRIPT;
    }
}
