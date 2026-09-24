package com.payevidence.web;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.payevidence.domain.ArtifactType;
import com.payevidence.web.error.ApiError;
import com.payevidence.web.error.NotFoundException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** JSON errors for REST APIs only — Thymeleaf MVC uses default error pages. */
@RestControllerAdvice(annotations = RestController.class)
public class ApiExceptionHandler {

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<Map<String, Object>> notFound(NotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiError.of(404, "not_found", e.getMessage()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiError.of(400, "bad_request", e.getMessage()));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<Map<String, Object>> unreadable(HttpMessageNotReadableException e) {
    String msg = "Malformed request body";
    Throwable cause = e.getMostSpecificCause();
    if (cause instanceof InvalidFormatException ife
        && ife.getTargetType() != null
        && ife.getTargetType().equals(ArtifactType.class)) {
      String allowed = Arrays.stream(ArtifactType.values())
          .map(Enum::name)
          .collect(Collectors.joining(", "));
      msg = "Invalid artifact type '" + ife.getValue() + "'. Allowed: " + allowed;
    } else if (cause != null && cause.getMessage() != null) {
      msg = cause.getMessage();
    }
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiError.of(400, "bad_request", msg));
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<Map<String, Object>> conflict(IllegalStateException e) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiError.of(500, "internal_error", e.getMessage()));
  }
}
