package com.nutrifit.app.ui;

import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nutrifit.app.R;

public final class Ui {
  private Ui() {}

  /** Display both stable meal keys and labels stored by previous app versions. */
  public static String meal(android.content.Context context, String stored) {
    String[] labels = context.getResources().getStringArray(R.array.meals);
    String[] keys = context.getResources().getStringArray(R.array.meal_keys);
    for (int i = 0; i < keys.length; i++) if (keys[i].equals(stored)) return labels[i];
    for (String tag : context.getResources().getStringArray(R.array.locale_tags)) {
      android.content.res.Configuration config =
          new android.content.res.Configuration(context.getResources().getConfiguration());
      config.setLocale(java.util.Locale.forLanguageTag(tag));
      String[] legacy =
          context.createConfigurationContext(config).getResources().getStringArray(R.array.meals);
      for (int i = 0; i < legacy.length; i++) if (legacy[i].equals(stored)) return labels[i];
    }
    return stored;
  }

  public static void insets(View root) {
    ViewCompat.setOnApplyWindowInsetsListener(
        root,
        (v, window) -> {
          Insets bars =
              window.getInsets(
                  WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
          v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
          return window;
        });
    ViewCompat.requestApplyInsets(root);
  }

  public static void text(View root, int id, String value) {
    ((TextView) root.findViewById(id)).setText(value);
  }

  public static String value(View root, int id) {
    return ((EditText) root.findViewById(id)).getText().toString().trim();
  }

  public static double number(View root, int id) {
    return Double.parseDouble(value(root, id).replace(',', '.'));
  }

  public static void spinner(View root, int id, int array, int selected) {
    Spinner spinner = root.findViewById(id);
    ArrayAdapter<CharSequence> adapter =
        ArrayAdapter.createFromResource(
            root.getContext(), array, android.R.layout.simple_spinner_item);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(adapter);
    spinner.setSelection(selected);
  }

  public static int selected(View root, int id) {
    return ((Spinner) root.findViewById(id)).getSelectedItemPosition();
  }

  public static void enabled(View v, boolean enabled) {
    v.setEnabled(enabled);
    if (v instanceof android.view.ViewGroup) {
      android.view.ViewGroup group = (android.view.ViewGroup) v;
      for (int i = 0; i < group.getChildCount(); i++) enabled(group.getChildAt(i), enabled);
    }
  }

  public static void error(AppCompatActivity activity, int message) {
    new MaterialAlertDialogBuilder(activity)
        .setTitle(R.string.check_data)
        .setMessage(message)
        .setPositiveButton(R.string.ok, null)
        .show();
  }
}
