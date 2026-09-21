package com.nutrifit.app.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.*;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nutrifit.app.R;
import com.nutrifit.app.data.LocalRepository;
import com.nutrifit.app.model.Profile;
import com.nutrifit.app.model.Recipe;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class RecipeActivity extends AppCompatActivity {
  private LocalRepository repo;
  private Recipe recipe;
  private Profile profile;
  private View root;
  private LocalDate day;
  private boolean busy;

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    repo = LocalRepository.get(this);
    if (!repo.hasAccess()) {
      finish();
      return;
    }
    setContentView(R.layout.activity_recipe);
    root = findViewById(R.id.root);
    Ui.insets(root);
    day =
        LocalDate.parse(state != null ? state.getString("day") : getIntent().getStringExtra("day"));
    root.findViewById(R.id.detail_back).setOnClickListener(v -> finish());
    Ui.enabled(root, false);
    String language = com.nutrifit.app.data.ContentTranslations.language(this);
    repo.io.execute(
        () -> {
          try {
            repo.library.initialize(language);
            recipe = repo.library.recipe(getIntent().getStringExtra("recipe"));
            profile = repo.profiles.load();
            runOnUiThread(
                () -> {
                  if (!isDestroyed()) {
                    Ui.enabled(root, true);
                    bind(state);
                  }
                });
          } catch (Exception e) {
            runOnUiThread(
                () -> {
                  if (!isDestroyed()) {
                    Ui.error(this, R.string.storage_error);
                    root.findViewById(R.id.detail_back).setEnabled(true);
                  }
                });
          }
        });
  }

  private void bind(Bundle state) {
    Ui.text(root, R.id.detail_title, recipe.title);
    Ui.text(
        root,
        R.id.detail_meta,
        recipe.product
            ? getString(R.string.product_meta, recipe.kcal)
            : getString(R.string.recipe_meta, recipe.minutes, recipe.kcal)
                + "\n"
                + getString(R.string.recipe_portion, recipe.grams));
    Ui.text(
        root,
        R.id.detail_nutrition,
        getString(
            R.string.recipe_nutrition, recipe.kcal, recipe.protein, recipe.fat, recipe.carbs));
    EditText amount = root.findViewById(R.id.servings);
    ((com.google.android.material.textfield.TextInputLayout) amount.getParent().getParent())
        .setHint(getString(recipe.product ? R.string.quantity_grams : R.string.servings));
    amount.setText(
        state != null
            ? state.getString("amount", recipe.product ? "100" : "1")
            : recipe.product ? "100" : "1");
    Ui.spinner(root, R.id.detail_meal, R.array.meals, state == null ? 0 : state.getInt("meal"));
    dateLabel();
    root.findViewById(R.id.detail_date)
        .setOnClickListener(
            v ->
                new DatePickerDialog(
                        this,
                        (p, y, m, d) -> {
                          day = LocalDate.of(y, m + 1, d);
                          dateLabel();
                        },
                        day.getYear(),
                        day.getMonthValue() - 1,
                        day.getDayOfMonth())
                    .show());
    amount.addTextChangedListener(
        new TextWatcher() {
          public void beforeTextChanged(CharSequence s, int st, int c, int a) {}

          public void onTextChanged(CharSequence s, int st, int b, int c) {
            scale();
          }

          public void afterTextChanged(Editable e) {}
        });
    StringBuilder steps = new StringBuilder();
    for (int i = 0; i < recipe.steps.size(); i++)
      steps.append(getString(R.string.recipe_step, i + 1, recipe.steps.get(i))).append("\n\n");
    Ui.text(root, R.id.detail_steps, steps.toString());
    root.findViewById(R.id.steps_heading).setVisibility(recipe.product ? View.GONE : View.VISIBLE);
    root.findViewById(R.id.ingredients_heading)
        .setVisibility(recipe.product ? View.GONE : View.VISIBLE);
    List<String> allergyNames = new ArrayList<>();
    String[] keys = com.nutrifit.app.data.Catalog.ALLERGENS,
        names = getResources().getStringArray(R.array.allergens);
    for (int i = 0; i < keys.length; i++)
      if (Arrays.asList(recipe.allergens.split(",")).contains(keys[i])) allergyNames.add(names[i]);
    Ui.text(
        root,
        R.id.detail_allergens,
        getString(
            R.string.allergy_label,
            allergyNames.isEmpty()
                ? getString(R.string.no_listed_allergens)
                : String.join(", ", allergyNames)));
    Ui.text(
        root,
        R.id.detail_note,
        getString(recipe.product ? R.string.product_note : R.string.nutrition_note));
    root.findViewById(R.id.detail_add).setOnClickListener(v -> save(false));
    root.findViewById(R.id.detail_plan).setOnClickListener(v -> save(true));
    scale();
  }

  private void dateLabel() {
    Ui.text(
        root,
        R.id.detail_date,
        day.format(
            DateTimeFormatter.ofPattern(
                "d MMMM yyyy", this.getResources().getConfiguration().getLocales().get(0))));
  }

  private double portions() {
    return Ui.number(root, R.id.servings) / (recipe.product ? 100 : 1);
  }

  private void scale() {
    try {
      double n = portions();
      if (!Double.isFinite(n) || n <= 0 || n > 100) {
        Ui.text(root, R.id.detail_scaled, getString(R.string.portion_range));
        return;
      }
      Ui.text(
          root,
          R.id.detail_scaled,
          getString(
              R.string.recipe_nutrition,
              recipe.kcal * n,
              recipe.protein * n,
              recipe.fat * n,
              recipe.carbs * n));
      StringBuilder ingredients = new StringBuilder();
      for (Recipe.Ingredient item : recipe.ingredients)
        ingredients
            .append(getString(R.string.ingredient_amount, item.name, item.grams * n))
            .append('\n');
      Ui.text(root, R.id.detail_ingredients, ingredients.toString());
    } catch (NumberFormatException e) {
      Ui.text(root, R.id.detail_scaled, getString(R.string.number_error));
    }
  }

  private void save(boolean plan) {
    if (busy) return;
    try {
      double n = portions();
      if (!Double.isFinite(n)
          || n < (recipe.product ? .01 : .25)
          || n > (recipe.product ? 20 : 10)
          || (!plan && recipe.grams * n > 2000)) {
        Ui.error(this, R.string.portion_range);
        return;
      }
      if (plan && (n < .25 || n > 10)) {
        Ui.error(this, R.string.portion_range);
        return;
      }
      if (!plan && day.isAfter(LocalDate.now())) {
        Ui.error(this, R.string.plan_future);
        return;
      }
      if (profile != null && !Collections.disjoint(recipe.food().allergens, profile.allergies)) {
        Ui.error(this, R.string.allergen_warning);
        return;
      }
      if (plan && !repo.premium()) {
        new MaterialAlertDialogBuilder(this)
            .setMessage(R.string.premium_gate)
            .setPositiveButton(R.string.ok, null)
            .show();
        return;
      }
      String meal =
          getResources().getStringArray(R.array.meal_keys)[Ui.selected(root, R.id.detail_meal)];
      String date = day.toString();
      busy = true;
      Ui.enabled(root, false);
      repo.io.execute(
          () -> {
            try {
              if (plan) repo.library.plan(date, meal, recipe.id, n);
              else repo.db.add(date, meal, recipe.food(), recipe.grams * n, recipe.title);
              runOnUiThread(
                  () -> {
                    if (!isDestroyed()) {
                      Toast.makeText(this, R.string.recipe_saved, Toast.LENGTH_SHORT).show();
                      finish();
                    }
                  });
            } catch (Exception e) {
              runOnUiThread(
                  () -> {
                    if (!isDestroyed()) {
                      busy = false;
                      Ui.enabled(root, true);
                      Ui.error(this, R.string.storage_error);
                    }
                  });
            }
          });
    } catch (NumberFormatException e) {
      Ui.error(this, R.string.number_error);
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    state.putString("day", day.toString());
    if (recipe != null) {
      state.putString("amount", Ui.value(root, R.id.servings));
      state.putInt("meal", Ui.selected(root, R.id.detail_meal));
    }
    super.onSaveInstanceState(state);
  }
}
