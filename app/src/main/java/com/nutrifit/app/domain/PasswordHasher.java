package com.nutrifit.app.domain;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Salted, deliberately expensive password derivation. Always call on the IO executor. */
public final class PasswordHasher {
  private static final int ITERATIONS = 210_000;

  public static String salt() {
    byte[] bytes = new byte[16];
    new SecureRandom().nextBytes(bytes);
    return Base64.getEncoder().encodeToString(bytes);
  }

  public static String hash(char[] password, String salt) throws Exception {
    PBEKeySpec spec = new PBEKeySpec(password, Base64.getDecoder().decode(salt), ITERATIONS, 256);
    try {
      return Base64.getEncoder()
          .encodeToString(
              SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                  .generateSecret(spec)
                  .getEncoded());
    } finally {
      spec.clearPassword();
      java.util.Arrays.fill(password, '\0');
    }
  }

  public static boolean verify(char[] password, String salt, String expected) throws Exception {
    return MessageDigest.isEqual(
        Base64.getDecoder().decode(hash(password, salt)), Base64.getDecoder().decode(expected));
  }
}
