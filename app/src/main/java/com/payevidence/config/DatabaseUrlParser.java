package com.payevidence.config;

import java.net.URI;

/**
 * Turns a Neon/Render {@code postgres://} or {@code postgresql://} URL, or an existing
 * {@code jdbc:postgresql:} URL, into datasource settings. Returns null for blank or
 * non-Postgres values so the app can keep its H2 default.
 */
public final class DatabaseUrlParser {
  public record PostgresTarget(String jdbcUrl, String username, String password) {}

  private DatabaseUrlParser() {}

  public static PostgresTarget parse(String databaseUrl) {
    if (databaseUrl == null || databaseUrl.isBlank()) {
      return null;
    }
    String url = databaseUrl.trim();
    if (url.startsWith("jdbc:postgresql:")) {
      return new PostgresTarget(url, null, null);
    }
    if (!url.startsWith("postgres://") && !url.startsWith("postgresql://")) {
      return null;
    }
    try {
      URI uri = URI.create(url);
      if (uri.getHost() == null || uri.getHost().isBlank()) {
        return null;
      }
      String path = uri.getPath();
      if (path == null || path.isBlank() || "/".equals(path)) {
        return null;
      }
      String userInfo = uri.getUserInfo();
      if (userInfo == null || userInfo.isBlank()) {
        return null;
      }
      int colon = userInfo.indexOf(':');
      String username = colon < 0 ? userInfo : userInfo.substring(0, colon);
      String password = colon < 0 ? "" : userInfo.substring(colon + 1);
      if (username.isBlank()) {
        return null;
      }
      StringBuilder jdbc = new StringBuilder("jdbc:postgresql://").append(uri.getHost());
      if (uri.getPort() > 0) {
        jdbc.append(':').append(uri.getPort());
      }
      jdbc.append(path);
      if (uri.getRawQuery() != null && !uri.getRawQuery().isBlank()) {
        jdbc.append('?').append(uri.getRawQuery());
      }
      return new PostgresTarget(jdbc.toString(), username, password);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }
}
