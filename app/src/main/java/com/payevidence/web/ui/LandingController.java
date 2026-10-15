package com.payevidence.web.ui;

import com.payevidence.service.SignupService;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LandingController {
  private final SignupService signupService;

  public LandingController(SignupService signupService) {
    this.signupService = signupService;
  }

  @GetMapping("/")
  public String landing(
      @RequestParam(name = "submitted", required = false) String submitted,
      @RequestParam(name = "already", required = false) String already,
      Model model) {
    prepare(model, submitted, already);
    return "landing";
  }

  @GetMapping("/signup")
  public String signup(
      @RequestParam(name = "submitted", required = false) String submitted,
      @RequestParam(name = "already", required = false) String already,
      Model model) {
    prepare(model, submitted, already);
    return "signup";
  }

  @PostMapping("/signup")
  public String submit(
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) String company,
      @RequestParam(required = false) String role,
      @RequestParam(required = false) String message,
      @RequestParam(defaultValue = "signup") String returnTo,
      Model model) {
    SignupForm form = new SignupForm(name, email, company, role, message);
    Map<String, String> errors = SignupForm.validate(form);
    boolean landing = "landing".equals(returnTo);
    if (!errors.isEmpty()) {
      model.addAttribute("form", form);
      model.addAttribute("errors", errors);
      model.addAttribute("submitted", false);
      return landing ? "landing" : "signup";
    }
    SignupService.Result result = signupService.register(form);
    boolean already = result == SignupService.Result.ALREADY_LISTED;
    String query = already ? "submitted=1&already=1" : "submitted=1";
    return landing ? "redirect:/?" + query + "#signup" : "redirect:/signup?" + query;
  }

  private static void prepare(Model model, String submitted, String already) {
    if (!model.containsAttribute("form")) {
      model.addAttribute("form", new SignupForm());
    }
    boolean ok = "1".equals(submitted) || Boolean.TRUE.equals(model.getAttribute("success"));
    model.addAttribute("submitted", ok);
    if ("1".equals(already) || Boolean.TRUE.equals(model.getAttribute("already"))) {
      model.addAttribute("already", true);
    }
  }
}
