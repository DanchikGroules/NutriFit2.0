package com.nutrifit.app.data;

import android.content.Context;
import android.content.SharedPreferences;
import com.nutrifit.app.domain.AccountRules;
import com.nutrifit.app.domain.PasswordHasher;
import java.util.Locale;

/** One account per installation. No networking and no plaintext password storage. */
public final class LocalAccount {
  private final SharedPreferences prefs;

  public LocalAccount(Context context) {
    prefs = context.getSharedPreferences("local_account", Context.MODE_PRIVATE);
  }

  public boolean exists() {
    return prefs.contains("hash");
  }

  public boolean signedIn() {
    return exists() && prefs.getBoolean("session", false);
  }

  public String name() {
    return prefs.getString("name", "");
  }

  public String email() {
    return prefs.getString("email", "");
  }

  public String register(String name, String email, char[] password) throws Exception {
    if (exists()) throw new IllegalStateException();
    AccountRules.password(password);
    email = AccountRules.email(email);
    name = name.trim();
    if (name.isEmpty() || name.length() > 50) throw new IllegalArgumentException("name");
    String salt = PasswordHasher.salt();
    String hash = PasswordHasher.hash(password, salt);
    String recovery = AccountRules.recovery(), recoverySalt = PasswordHasher.salt();
    String recoveryHash =
        PasswordHasher.hash(AccountRules.normalizeCode(recovery).toCharArray(), recoverySalt);
    if (!prefs
        .edit()
        .putString("name", name)
        .putString("email", email.trim().toLowerCase(Locale.ROOT))
        .putString("salt", salt)
        .putString("hash", hash)
        .putString("recovery_salt", recoverySalt)
        .putString("recovery_hash", recoveryHash)
        .putBoolean("session", true)
        .commit()) throw new java.io.IOException();
    return recovery;
  }

  public boolean login(String email, char[] password) throws Exception {
    checkDelay();
    boolean correct =
        PasswordHasher.verify(password, prefs.getString("salt", ""), prefs.getString("hash", ""));
    if (!correct || !email.trim().toLowerCase(Locale.ROOT).equals(email())) {
      failed();
      return false;
    }
    if (!prefs
        .edit()
        .putBoolean("session", true)
        .putInt("failures", 0)
        .putLong("locked_until", 0)
        .commit()) throw new java.io.IOException();
    return true;
  }

  public void logout() {
    if (!prefs.edit().putBoolean("session", false).commit()) throw new IllegalStateException();
  }

  private void checkDelay() {
    if (System.currentTimeMillis() < prefs.getLong("locked_until", 0))
      throw new IllegalStateException("throttled");
  }

  private void failed() {
    int attempts = prefs.getInt("failures", 0) + 1;
    if (!prefs
        .edit()
        .putInt("failures", attempts >= 5 ? 0 : attempts)
        .putLong("locked_until", attempts >= 5 ? System.currentTimeMillis() + 30000 : 0)
        .commit()) throw new IllegalStateException("write_failed");
  }

  public boolean verify(char[] password) throws Exception {
    checkDelay();
    boolean ok =
        PasswordHasher.verify(password, prefs.getString("salt", ""), prefs.getString("hash", ""));
    if (!ok) failed();
    return ok;
  }

  public boolean changePassword(char[] current, char[] next) throws Exception {
    try {
      AccountRules.password(next);
      if (!verify(current)) return false;
      setPassword(next, false);
      return true;
    } finally {
      java.util.Arrays.fill(current, '\0');
      java.util.Arrays.fill(next, '\0');
    }
  }

  private void setPassword(char[] next, boolean logout) throws Exception {
    String salt = PasswordHasher.salt(), hash = PasswordHasher.hash(next, salt);
    if (!prefs
        .edit()
        .putString("salt", salt)
        .putString("hash", hash)
        .putBoolean("session", !logout)
        .putInt("failures", 0)
        .putLong("locked_until", 0)
        .commit()) throw new java.io.IOException();
  }

  public String renewRecovery(char[] current) throws Exception {
    try {
      if (!verify(current)) return null;
      String code = AccountRules.recovery(), salt = PasswordHasher.salt();
      String hash = PasswordHasher.hash(AccountRules.normalizeCode(code).toCharArray(), salt);
      if (!prefs.edit().putString("recovery_salt", salt).putString("recovery_hash", hash).commit())
        throw new java.io.IOException();
      return code;
    } finally {
      java.util.Arrays.fill(current, '\0');
    }
  }

  public boolean reset(String email, String code, char[] next) throws Exception {
    try {
      checkDelay();
      AccountRules.password(next);
      if (!prefs.contains("recovery_hash")) {
        failed();
        return false;
      }
      boolean valid =
          PasswordHasher.verify(
              AccountRules.normalizeCode(code).toCharArray(),
              prefs.getString("recovery_salt", ""),
              prefs.getString("recovery_hash", ""));
      if (!valid || !AccountRules.email(email).equals(email())) {
        failed();
        return false;
      }
      setPassword(next, true);
      return true;
    } finally {
      java.util.Arrays.fill(next, '\0');
    }
  }
}
