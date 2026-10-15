package com.payevidence.web.ui;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Early-access form. Not a JPA entity — {@code Signup} is the stored row. */
public class SignupForm implements Serializable {
  private static final long serialVersionUID = 1L;
  static final int NAME_MAX = 120;
  static final int EMAIL_MAX = 200;
  static final int COMPANY_MAX = 160;
  static final int ROLE_MAX = 120;
  static final int MESSAGE_MAX = 2000;

  private static final Pattern EMAIL = Pattern.compile(
      "^[A-Z0-9._%+-]+@[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?(?:\\.[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?)+$",
      Pattern.CASE_INSENSITIVE);

  private String name = "";
  private String email = "";
  private String company = "";
  private String role = "";
  private String message = "";

  public SignupForm() {}

  public SignupForm(String name, String email, String company, String role, String message) {
    this.name = name == null ? "" : name;
    this.email = email == null ? "" : email;
    this.company = company == null ? "" : company;
    this.role = role == null ? "" : role;
    this.message = message == null ? "" : message;
  }

  public static Map<String, String> validate(SignupForm form) {
    Map<String, String> errors = new LinkedHashMap<>();
    String name = trim(form.getName());
    String email = trim(form.getEmail());
    String company = trim(form.getCompany());
    String role = trim(form.getRole());
    String message = form.getMessage() == null ? "" : form.getMessage().trim();

    if (name.isEmpty()) {
      errors.put("name", "Enter your name.");
    } else if (name.length() > NAME_MAX) {
      errors.put("name", "Name is too long.");
    }
    if (email.isEmpty()) {
      errors.put("email", "Enter your work email.");
    } else if (email.length() > EMAIL_MAX || !EMAIL.matcher(email).matches()) {
      errors.put("email", "Enter a valid work email.");
    }
    if (company.isEmpty()) {
      errors.put("company", "Enter your company.");
    } else if (company.length() > COMPANY_MAX) {
      errors.put("company", "Company is too long.");
    }
    if (role.length() > ROLE_MAX) {
      errors.put("role", "Role is too long.");
    }
    if (message.length() > MESSAGE_MAX) {
      errors.put("message", "Message is too long (2000 characters max).");
    }
    return errors;
  }

  public String normalizedEmail() {
    return trim(email).toLowerCase(Locale.ROOT);
  }

  private static String trim(String value) {
    return value == null ? "" : value.trim();
  }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public String getCompany() { return company; }
  public void setCompany(String company) { this.company = company; }
  public String getRole() { return role; }
  public void setRole(String role) { this.role = role; }
  public String getMessage() { return message; }
  public void setMessage(String message) { this.message = message; }
}
