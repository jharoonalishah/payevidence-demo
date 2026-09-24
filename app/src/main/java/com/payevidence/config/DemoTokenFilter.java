package com.payevidence.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Protects demo UI pages with a shared secret. REST APIs under {@code /api/**} stay open
 * for local curl (documented in README / DEPLOY.md). Production VPS should add basic-auth
 * or TLS reverse-proxy in front of everything.
 *
 * Accepted credentials: query {@code ?token=...} or cookie {@code payevidence_demo}.
 * Matching query token is also written as an HttpOnly cookie for subsequent clicks.
 */
@Component
@Order(1)
public class DemoTokenFilter extends OncePerRequestFilter {
  public static final String COOKIE_NAME = "payevidence_demo";
  public static final String QUERY_PARAM = "token";

  private final PayEvidenceProperties properties;

  public DemoTokenFilter(PayEvidenceProperties properties) {
    this.properties = properties;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    if (path == null) {
      return true;
    }
    // APIs open for curl; static assets and gate form unauthenticated
    if (path.startsWith("/api/")) return true;
    if (path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/")) return true;
    if (path.startsWith("/h2-console")) return true;
    if (path.equals("/demo/gate") || path.equals("/error")) return true;
    // Only gate the UI surface
    if (path.equals("/") || path.equals("/demo") || path.startsWith("/demo/")) return false;
    return true;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String expected = properties.getDemoToken();
    if (expected == null || expected.isBlank()) {
      filterChain.doFilter(request, response);
      return;
    }

    String queryToken = request.getParameter(QUERY_PARAM);
    if (queryToken != null && constantTimeEquals(queryToken, expected)) {
      Cookie cookie = new Cookie(COOKIE_NAME, expected);
      cookie.setPath("/");
      cookie.setHttpOnly(true);
      cookie.setMaxAge(60 * 60 * 12); // 12h demo session
      response.addCookie(cookie);
      filterChain.doFilter(request, response);
      return;
    }

    if (cookieMatches(request, expected)) {
      filterChain.doFilter(request, response);
      return;
    }

    // Redirect to gate (preserve intended path)
    String target = request.getRequestURI();
    String q = request.getQueryString();
    if (q != null && !q.isBlank()) {
      target = target + "?" + q;
    }
    response.setHeader(HttpHeaders.LOCATION, "/demo/gate?next=" + urlEncode(target));
    response.setStatus(HttpServletResponse.SC_FOUND);
  }

  private boolean cookieMatches(HttpServletRequest request, String expected) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) return false;
    return Arrays.stream(cookies)
        .filter(c -> COOKIE_NAME.equals(c.getName()))
        .anyMatch(c -> constantTimeEquals(c.getValue(), expected));
  }

  private static boolean constantTimeEquals(String a, String b) {
    if (a == null || b == null) return false;
    byte[] x = a.getBytes(StandardCharsets.UTF_8);
    byte[] y = b.getBytes(StandardCharsets.UTF_8);
    return MessageDigest.isEqual(x, y);
  }

  private static String urlEncode(String s) {
    return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
  }
}
