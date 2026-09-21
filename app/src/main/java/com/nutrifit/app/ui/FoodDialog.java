package com.nutrifit.app.ui;

import android.view.View;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nutrifit.app.R;
import com.nutrifit.app.data.Catalog;
import com.nutrifit.app.model.Food;
import java.util.Collections;

final class FoodDialog {
  private final MainActivity a;
  private final Food selected;
  private final int portion;

  FoodDialog(MainActivity a, Food selected, int portion) {
    this.a = a;
    this.selected = selected;
    this.portion = portion;
  }

  void show() {
    View root = a.getLayoutInflater().inflate(R.layout.dialog_food, null);
    Ui.spinner(
        root, R.id.food, R.array.foods, selected == null ? 0 : Catalog.FOODS.indexOf(selected));
    Ui.spinner(root, R.id.meal, R.array.meals, 0);
    Ui.text(root, R.id.grams, Integer.toString(portion));
    AlertDialog dialog =
        new MaterialAlertDialogBuilder(a)
            .setTitle(R.string.add_food)
            .setView(root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create();
    String day = a.day.toString();
    dialog.show();
    dialog
        .getButton(AlertDialog.BUTTON_POSITIVE)
        .setOnClickListener(
            v -> {
              try {
                double amount = Ui.number(root, R.id.grams);
                if (!Double.isFinite(amount) || amount < 1 || amount > 2000) {
                  Ui.error(a, R.string.portion_error);
                  return;
                }
                Food food = Catalog.FOODS.get(Ui.selected(root, R.id.food));
                if (!Collections.disjoint(food.allergens, a.profile.allergies)) {
                  Ui.error(a, R.string.allergen_error);
                  return;
                }
                String meal =
                    a.getResources()
                        .getStringArray(R.array.meal_keys)[Ui.selected(root, R.id.meal)];
                String name =
                    a.getResources().getStringArray(R.array.foods)[Ui.selected(root, R.id.food)];
                a.write(
                    () -> a.repo.db.add(day, meal, food, amount, name),
                    () -> a.show(R.id.nav_diary));
                dialog.dismiss();
              } catch (NumberFormatException e) {
                Ui.error(a, R.string.number_error);
              }
            });
  }
}
