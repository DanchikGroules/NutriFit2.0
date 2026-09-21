package com.nutrifit.app.ui;

import android.app.DatePickerDialog;
import android.view.View;
import android.widget.LinearLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.nutrifit.app.R;
import com.nutrifit.app.data.AppDatabase;
import com.nutrifit.app.data.Catalog;
import com.nutrifit.app.domain.NutritionCalculator;
import com.nutrifit.app.domain.RecommendationEngine;
import com.nutrifit.app.model.Food;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

final class DiaryScreen {
  private final MainActivity a;
  private final View root;

  DiaryScreen(MainActivity activity) {
    a = activity;
    root = a.layout(R.layout.screen_diary);
    Ui.text(root, R.id.greeting, a.getString(R.string.greeting, a.profile.name));
    Ui.text(
        root,
        R.id.date,
        a.day.format(
            DateTimeFormatter.ofPattern(
                "d MMMM yyyy", a.getResources().getConfiguration().getLocales().get(0))));
    root.findViewById(R.id.past_note)
        .setVisibility(a.day.equals(LocalDate.now()) ? View.GONE : View.VISIBLE);
    root.findViewById(R.id.date)
        .setOnClickListener(
            v -> {
              DatePickerDialog dialog =
                  new DatePickerDialog(
                      a,
                      (picker, y, m, d) -> {
                        a.day = LocalDate.of(y, m + 1, d);
                        a.show(R.id.nav_diary);
                      },
                      a.day.getYear(),
                      a.day.getMonthValue() - 1,
                      a.day.getDayOfMonth());
              dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
              dialog.show();
            });
    root.findViewById(R.id.add_food).setOnClickListener(v -> a.show(R.id.nav_library));
    root.findViewById(R.id.discover).setOnClickListener(v -> a.show(R.id.nav_library));
    root.findViewById(R.id.view_progress).setOnClickListener(v -> a.show(R.id.nav_progress));
    String waterDay = a.day.toString();
    root.findViewById(R.id.water_add)
        .setOnClickListener(
            v -> a.write(() -> a.repo.library.water(waterDay, 250), () -> a.show(R.id.nav_diary)));
    root.findViewById(R.id.water_remove)
        .setOnClickListener(
            v -> a.write(() -> a.repo.library.water(waterDay, -250), () -> a.show(R.id.nav_diary)));
    int token = a.token();
    String day = a.day.toString();
    a.setBusy(true);
    a.repo.io.execute(
        () -> {
          try {
            List<AppDatabase.Entry> entries = a.repo.db.diary(day);
            int water = a.repo.library.water(day);
            a.runOnUiThread(
                () -> {
                  if (a.active(token)) {
                    a.setBusy(false);
                    render(entries);
                    Ui.text(root, R.id.water_value, a.getString(R.string.water_value, water));
                  }
                });
          } catch (Exception e) {
            a.runOnUiThread(
                () -> {
                  if (a.active(token)) {
                    a.setBusy(false);
                    Ui.error(a, R.string.storage_error);
                  }
                });
          }
        });
  }

  private void render(List<AppDatabase.Entry> entries) {
    NutritionCalculator.Target target = NutritionCalculator.calculate(a.profile);
    double kcal = 0, protein = 0, fat = 0, carbs = 0;
    for (AppDatabase.Entry e : entries) {
      kcal += e.kcal;
      protein += e.protein;
      fat += e.fat;
      carbs += e.carbs;
    }
    double remaining = target.calories - kcal;
    Ui.text(root, R.id.calories, a.getString(R.string.calorie_total, kcal, target.calories));
    Ui.text(
        root,
        R.id.remaining,
        a.getString(
            remaining >= 0 ? R.string.remaining : R.string.over_target, Math.abs(remaining)));
    Ui.text(root, R.id.protein_value, a.getString(R.string.macro_value, protein, target.protein));
    Ui.text(root, R.id.fat_value, a.getString(R.string.macro_value, fat, target.fat));
    Ui.text(root, R.id.carbs_value, a.getString(R.string.macro_value, carbs, target.carbs));
    ((LinearProgressIndicator) root.findViewById(R.id.calorie_progress))
        .setProgressCompat((int) Math.min(100, 100 * kcal / target.calories), true);
    LinearLayout list = root.findViewById(R.id.entries);
    if (entries.isEmpty()) a.getLayoutInflater().inflate(R.layout.empty_diary, list, true);
    for (AppDatabase.Entry e : entries) {
      View row = a.getLayoutInflater().inflate(R.layout.row_food, list, false);
      list.addView(row);
      String displayedName = a.repo.library.displayName(e.name);
      Ui.text(row, R.id.row_name, displayedName);
      Ui.text(
          row,
          R.id.row_detail,
          a.getString(R.string.food_detail, Ui.meal(a, e.meal), e.grams, e.kcal));
      row.findViewById(R.id.row_action)
          .setOnClickListener(
              v ->
                  new MaterialAlertDialogBuilder(a)
                      .setMessage(a.getString(R.string.delete_question, displayedName))
                      .setNegativeButton(R.string.cancel, null)
                      .setPositiveButton(
                          R.string.delete,
                          (d, w) ->
                              a.write(() -> a.repo.db.delete(e.id), () -> a.show(R.id.nav_diary)))
                      .show());
    }
    LinearLayout suggestions = root.findViewById(R.id.suggestions);
    if (!a.day.equals(LocalDate.now())) {
      root.findViewById(R.id.suggestions_heading).setVisibility(View.GONE);
      return;
    }
    List<RecommendationEngine.Suggestion> results =
        RecommendationEngine.recommend(Catalog.FOODS, a.profile, remaining);
    if (results.isEmpty()) {
      View row = a.getLayoutInflater().inflate(R.layout.row_weight, suggestions, false);
      suggestions.addView(row);
      Ui.text(row, R.id.row_weight, a.getString(R.string.no_suggestions));
    }
    for (RecommendationEngine.Suggestion s : results) {
      View row = a.getLayoutInflater().inflate(R.layout.row_food, suggestions, false);
      suggestions.addView(row);
      Ui.text(row, R.id.row_name, foodName(s.food));
      Ui.text(row, R.id.row_detail, a.getString(R.string.suggestion_detail, s.grams, s.calories));
      Ui.text(row, R.id.row_action, a.getString(R.string.add_food));
      row.findViewById(R.id.row_action)
          .setOnClickListener(v -> new FoodDialog(a, s.food, s.grams).show());
    }
  }

  private String foodName(Food food) {
    return a.getResources().getStringArray(R.array.foods)[Catalog.FOODS.indexOf(food)];
  }
}
