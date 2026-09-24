package com.payevidence.web.error;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Stable error body for API responses. */
public final class ApiError {
  private ApiError() {}

  public static Map<String, Object> of(int status, String error, String message) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("status", status);
    body.put("error", error);
    body.put("message", message);
    body.put("timestamp", Instant.now().toString());
    return body;
  }
}
