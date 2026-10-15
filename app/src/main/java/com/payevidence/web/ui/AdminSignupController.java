package com.payevidence.web.ui;

import com.payevidence.config.PayEvidenceProperties;
import com.payevidence.repo.SignupRepository;
import com.payevidence.service.SignupService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminSignupController {
  static final String COOKIE_NAME = "payevidence_admin";

  private final PayEvidenceProperties properties;
  private final SignupRepository signupRepository;
  private final SignupService signupService;

  public AdminSignupController(
      PayEvidenceProperties properties,
      SignupRepository signupRepository,
      SignupService signupService) {
    this.properties = properties;
    this.signupRepository = signupRepository;
    this.signupService = signupService;
  }

  @GetMapping("/admin/signups")
  public String list(
      @RequestParam(required = false) String token,
      @RequestParam(required = false) String error,
      HttpServletRequest request,
      Model model) {
    String expected = requireToken();
    if (!authorized(request, token, expected)) {
      model.addAttribute("error", error);
      return "admin-gate";
    }
    model.addAttribute("signups", signupRepository.findAllByOrderByCreatedAtDesc());
    model.addAttribute("store", signupService.isPostgres()
        ? "Postgres (DATABASE_URL)"
        : "Local H2 — not durable on Render. Set DATABASE_URL.");
    return "admin-signups";
  }

  @PostMapping("/admin/signups")
  public String login(
      @RequestParam(required = false) String token,
      HttpServletResponse response,
      RedirectAttributes redirect) {
    String expected = requireToken();
    if (token != null && constantTimeEquals(token, expected)) {
      Cookie cookie = new Cookie(COOKIE_NAME, expected);
      cookie.setPath("/admin");
      cookie.setHttpOnly(true);
      cookie.setMaxAge(60 * 60 * 8);
      response.addCookie(cookie);
      return "redirect:/admin/signups";
    }
    redirect.addAttribute("error", "invalid");
    return "redirect:/admin/signups";
  }

  private String requireToken() {
    String expected = properties.getAdminToken();
    if (expected == null || expected.isBlank()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    return expected;
  }

  private boolean authorized(HttpServletRequest request, String queryToken, String expected) {
    if (queryToken != null && constantTimeEquals(queryToken, expected)) {
      return true;
    }
    String header = request.getHeader("Authorization");
    if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
      String bearer = header.substring(7).trim();
      if (constantTimeEquals(bearer, expected)) {
        return true;
      }
    }
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return false;
    }
    return Arrays.stream(cookies)
        .filter(c -> COOKIE_NAME.equals(c.getName()))
        .anyMatch(c -> constantTimeEquals(c.getValue(), expected));
  }

  static boolean constantTimeEquals(String a, String b) {
    if (a == null || b == null) {
      return false;
    }
    return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
  }
}
