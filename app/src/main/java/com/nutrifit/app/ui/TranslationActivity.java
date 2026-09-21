package com.nutrifit.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.*;
import androidx.core.os.LocaleListCompat;
import androidx.lifecycle.ViewModelProvider;
import com.nutrifit.app.R;

public final class TranslationActivity extends AppCompatActivity {
  private String language;

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    language = getIntent().getStringExtra("language");
    if (!"en".equals(language) && !"pl".equals(language)) {
      finish();
      return;
    }
    setContentView(R.layout.activity_translation);
    Ui.insets(findViewById(R.id.root));
    TranslationViewModel model = new ViewModelProvider(this).get(TranslationViewModel.class);
    TextView status = findViewById(R.id.translation_status);
    ProgressBar bar = findViewById(R.id.translation_progress);
    Button start = findViewById(R.id.translation_start);
    start.setOnClickListener(v -> model.start(language));
    findViewById(R.id.translation_offline).setOnClickListener(v -> applyLanguage());
    model.state.observe(
        this,
        value -> {
          boolean busy = value == 1 || value == 2;
          start.setEnabled(!busy);
          start.setText(value == 4 ? R.string.translation_retry : R.string.translation_start);
          bar.setVisibility(busy ? View.VISIBLE : View.GONE);
          bar.setIndeterminate(value == 1);
          status.setText(
              value == 1
                  ? R.string.translation_downloading
                  : value == 2
                      ? R.string.translation_working
                      : value == 4 ? R.string.translation_error : R.string.translation_ready);
          if (value == 3) applyLanguage();
        });
    model.progress.observe(
        this,
        p -> {
          if (Integer.valueOf(2).equals(model.state.getValue())) {
            bar.setMax(p[1]);
            bar.setProgress(p[0]);
            status.setText(getString(R.string.translation_count, p[0], p[1]));
          }
        });
  }

  private void applyLanguage() {
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language));
    startActivity(
        new Intent(this, AuthActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    finish();
  }
}
