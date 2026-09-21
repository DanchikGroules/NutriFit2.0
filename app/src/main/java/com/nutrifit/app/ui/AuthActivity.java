package com.nutrifit.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.textfield.TextInputLayout;
import com.nutrifit.app.R;
import com.nutrifit.app.data.LocalAccount;
import com.nutrifit.app.data.LocalRepository;

public class AuthActivity extends AppCompatActivity {
  private View root;

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    LocalAccount account = LocalRepository.get(this).account;
    if (LocalRepository.get(this).hasAccess()) {
      enter();
      return;
    }
    setContentView(R.layout.activity_auth);
    root = findViewById(R.id.root);
    Ui.insets(root);
    StartupMotion.play(root);
    root.findViewById(R.id.language).setOnClickListener(v -> LanguageSettings.show(this));
    boolean register = !account.exists();
    if (!register) {
      Ui.text(root, R.id.auth_title, getString(R.string.login_title));
      Ui.text(root, R.id.auth_subtitle, getString(R.string.login_subtitle));
      Ui.text(root, R.id.submit, getString(R.string.login));
      root.findViewById(R.id.name_layout).setVisibility(View.GONE);
      root.findViewById(R.id.confirm_layout).setVisibility(View.GONE);
      root.findViewById(R.id.password_hint).setVisibility(View.GONE);
      Ui.text(root, R.id.email, account.email());
    }
    root.findViewById(R.id.forgot).setVisibility(register ? View.GONE : View.VISIBLE);
    root.findViewById(R.id.forgot)
        .setOnClickListener(v -> AccountDialogs.reset(this, Ui.value(root, R.id.email)));
    AuthViewModel model = new ViewModelProvider(this).get(AuthViewModel.class);
    root.findViewById(R.id.demo_entry).setOnClickListener(v->model.demo(getString(R.string.demo_name),com.nutrifit.app.data.ContentTranslations.language(this)));
    model.state.observe(
        this,
        status -> {
          if (status == 2) {
            if (model.recoveryCode != null)
              AccountDialogs.showRecovery(
                  this,
                  model.recoveryCode,
                  () -> {
                    model.recoveryCode = null;
                    enter();
                  });
            else enter();
            return;
          }
          boolean busy = status == 1;
          Ui.enabled(root, !busy);
          root.findViewById(R.id.auth_busy).setVisibility(busy ? View.VISIBLE : View.GONE);
          root.findViewById(R.id.auth_error).setVisibility(status >= 3 ? View.VISIBLE : View.GONE);
          if (status >= 3)
            Ui.text(
                root,
                R.id.auth_error,
                getString(
                    status == 3
                        ? R.string.login_error
                        : status == 5 ? R.string.login_throttled : R.string.storage_error));
        });
    findViewById(R.id.submit)
        .setOnClickListener(
            v -> {
              for (int id :
                  new int[] {
                    R.id.name_layout, R.id.email_layout, R.id.password_layout, R.id.confirm_layout
                  }) ((TextInputLayout) findViewById(id)).setError(null);
              String name = Ui.value(root, R.id.name), email = Ui.value(root, R.id.email);
              String password = ((EditText) findViewById(R.id.password)).getText().toString();
              if (register && (name.isEmpty() || name.length() > 50)) {
                invalid(R.id.name_layout, R.string.name_error);
                return;
              }
              if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                invalid(R.id.email_layout, R.string.email_error);
                return;
              }
              if (password.length() < 8 || password.length() > 128) {
                invalid(R.id.password_layout, R.string.password_error);
                return;
              }
              if (register
                  && !password.equals(
                      ((EditText) findViewById(R.id.confirm)).getText().toString())) {
                invalid(R.id.confirm_layout, R.string.confirm_error);
                return;
              }
              model.submit(register, name, email, password.toCharArray());
              ((EditText) findViewById(R.id.password)).setText("");
              ((EditText) findViewById(R.id.confirm)).setText("");
            });
  }

  private void invalid(int id, int message) {
    TextInputLayout input = findViewById(id);
    input.setError(getString(message));
    input.requestFocus();
  }

  private void enter() {
    startActivity(
        new Intent(this, MainActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    finish();
  }
}
