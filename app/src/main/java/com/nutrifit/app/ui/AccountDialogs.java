package com.nutrifit.app.ui;

import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nutrifit.app.R;
import com.nutrifit.app.data.LocalRepository;

final class AccountDialogs {
  static void showRecovery(AppCompatActivity a, String code, Runnable after) {
    AlertDialog d =
        new MaterialAlertDialogBuilder(a)
            .setTitle(R.string.recovery_title)
            .setMessage(a.getString(R.string.recovery_explain) + "\n\n" + code)
            .setCancelable(false)
            .setPositiveButton(R.string.recovery_saved, (dialog, w) -> after.run())
            .show();
    TextView text = d.findViewById(android.R.id.message);
    if (text != null) text.setTextIsSelectable(true);
  }

  static void change(MainActivity a, boolean recovery) {
    form(a, null, recovery ? 1 : 0);
  }

  static void reset(AppCompatActivity a, String email) {
    form(a, email, 2);
  }

  private static void form(AppCompatActivity a, String email, int mode) {
    LocalRepository repo = LocalRepository.get(a);
    View root = a.getLayoutInflater().inflate(R.layout.dialog_security, null);
    visible(root, R.id.old_password, mode != 2);
    visible(root, R.id.updated_password, mode != 1);
    visible(root, R.id.updated_confirm, mode != 1);
    visible(root, R.id.recovery_input, mode == 2);
    AlertDialog dialog =
        new MaterialAlertDialogBuilder(a)
            .setTitle(
                mode == 0
                    ? R.string.change_password
                    : mode == 1 ? R.string.recovery_create : R.string.reset_password)
            .setView(root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create();
    dialog.show();
    dialog
        .getButton(AlertDialog.BUTTON_POSITIVE)
        .setOnClickListener(
            v -> {
              String old = ((EditText) root.findViewById(R.id.old_password)).getText().toString(),
                  next = ((EditText) root.findViewById(R.id.updated_password)).getText().toString();
              if (mode != 1 && (next.length() < 8 || next.length() > 128)) {
                Ui.error(a, R.string.password_error);
                return;
              }
              if (mode != 1
                  && !next.equals(
                      ((EditText) root.findViewById(R.id.updated_confirm)).getText().toString())) {
                Ui.error(a, R.string.confirm_error);
                return;
              }
              String code = Ui.value(root, R.id.recovery_input);
              dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
              dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
              dialog.setCancelable(false);
              repo.io.execute(
                  () -> {
                    try {
                      String recovery = null;
                      boolean ok;
                      if (mode == 1) {
                        recovery = repo.account.renewRecovery(old.toCharArray());
                        ok = recovery != null;
                      } else if (mode == 2)
                        ok = repo.account.reset(email, code, next.toCharArray());
                      else ok = repo.account.changePassword(old.toCharArray(), next.toCharArray());
                      String result = recovery;
                      a.runOnUiThread(
                          () -> {
                            if (a.isDestroyed()) return;
                            dialog.dismiss();
                            if (!ok)
                              Ui.error(
                                  a, mode == 2 ? R.string.reset_error : R.string.credentials_error);
                            else if (result != null) showRecovery(a, result, () -> {});
                            else
                              Toast.makeText(
                                      a,
                                      mode == 2
                                          ? R.string.reset_success
                                          : R.string.password_changed,
                                      Toast.LENGTH_LONG)
                                  .show();
                          });
                    } catch (Exception e) {
                      a.runOnUiThread(
                          () -> {
                            if (!a.isDestroyed()) {
                              dialog.dismiss();
                              Ui.error(
                                  a,
                                  "throttled".equals(e.getMessage())
                                      ? R.string.login_throttled
                                      : R.string.storage_error);
                            }
                          });
                    }
                  });
            });
  }

  private static void visible(View root, int id, boolean on) {
    ((View) root.findViewById(id).getParent().getParent())
        .setVisibility(on ? View.VISIBLE : View.GONE);
  }
}
// лох228-m-