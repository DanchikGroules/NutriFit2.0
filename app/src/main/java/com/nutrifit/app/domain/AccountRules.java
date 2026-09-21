package com.nutrifit.app.domain;

import java.security.SecureRandom;
import java.util.Locale;

public final class AccountRules {
  public static String email(String email) {
    String normalized = email.trim().toLowerCase(Locale.ROOT);
    if (normalized.length() > 254 || !normalized.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
      throw new IllegalArgumentException("email");
    return normalized;
  }

  public static void password(char[] value) {
    if (value.length < 8 || value.length > 128) throw new IllegalArgumentException("password");
  }

  public static String recovery() {
    byte[] bytes = new byte[16];
    new SecureRandom().nextBytes(bytes);
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < bytes.length; i++) {
      if (i > 0 && i % 2 == 0) result.append('-');
      result.append(String.format(Locale.ROOT, "%02X", bytes[i] & 255));
    }
    return result.toString();
  }

  public static String normalizeCode(String value) {
    return value.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
  }
}
