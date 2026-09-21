package com.nutrifit.app;

import static org.junit.Assert.*;

import com.nutrifit.app.domain.PasswordHasher;
import org.junit.Test;

public class PasswordHasherTest {
  @Test
  public void correctPasswordAcceptedAndWrongPasswordRejected() throws Exception {
    String salt = PasswordHasher.salt();
    String hash = PasswordHasher.hash("school-demo-2026".toCharArray(), salt);
    assertTrue(PasswordHasher.verify("school-demo-2026".toCharArray(), salt, hash));
    assertFalse(PasswordHasher.verify("wrong-password".toCharArray(), salt, hash));
  }

  @Test
  public void saltsProduceDifferentHashesAndInputIsCleared() throws Exception {
    String first = PasswordHasher.salt(), second = PasswordHasher.salt();
    assertNotEquals(first, second);
    char[] input = "same-password".toCharArray();
    String hash = PasswordHasher.hash(input, first);
    for (char value : input) assertEquals('\0', value);
    assertNotEquals(hash, PasswordHasher.hash("same-password".toCharArray(), second));
  }

  @Test
  public void unicodePasswordRoundTripsWithoutTrimming() throws Exception {
    String salt = PasswordHasher.salt(), password = " пароль-ученика ";
    String hash = PasswordHasher.hash(password.toCharArray(), salt);
    assertTrue(PasswordHasher.verify(password.toCharArray(), salt, hash));
    assertFalse(PasswordHasher.verify(password.trim().toCharArray(), salt, hash));
  }
}
