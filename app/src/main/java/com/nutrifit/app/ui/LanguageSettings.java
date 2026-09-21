package com.nutrifit.app.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nutrifit.app.R;

/** Android per-app languages; AppCompat persists the selection on Android 8–12 too. */
final class LanguageSettings {
  static void show(AppCompatActivity activity) {
    String current = activity.getResources().getConfiguration().getLocales().get(0).getLanguage();
    new MaterialAlertDialogBuilder(activity)
        .setTitle(R.string.language_title)
        .setSingleChoiceItems(
            R.array.language_names,
            current.equals("pl") ? 2 : current.equals("en") ? 1 : 0,
            (dialog, which) -> {
              dialog.dismiss();
              if (which != 0) {
                activity.startActivity(
                    new android.content.Intent(activity, TranslationActivity.class)
                        .putExtra("language", which == 2 ? "pl" : "en"));
                return;
              }
              AppCompatDelegate.setApplicationLocales(
                  LocaleListCompat.forLanguageTags(which == 2 ? "pl" : which == 1 ? "en" : "ru"));
            })
        .setNegativeButton(R.string.cancel, null)
        .show();
  }
}
