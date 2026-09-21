package com.nutrifit.app;

import static org.junit.Assert.*;

import com.nutrifit.app.domain.AccountRules;
import org.junit.Test;

public class AccountRulesTest {
  @Test
  public void emailNormalization() {
    assertEquals("demo@example.com", AccountRules.email(" Demo@Example.COM "));
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsBadEmail() {
    AccountRules.email("missing-at.example.com");
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsShortPassword() {
    AccountRules.password("123".toCharArray());
  }

  @Test
  public void recoveryCodesAreRandomAndNormalize() {
    String a = AccountRules.recovery(), b = AccountRules.recovery();
    assertNotEquals(a, b);
    assertEquals(32, AccountRules.normalizeCode(a).length());
    assertEquals(
        AccountRules.normalizeCode(a),
        AccountRules.normalizeCode(" " + a.toLowerCase(java.util.Locale.ROOT) + " "));
  }
}
