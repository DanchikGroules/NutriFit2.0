package com.nutrifit.app.ui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nutrifit.app.R;
import com.nutrifit.app.data.LibraryStore;
import java.time.LocalDate;
import java.util.*;

final class PlanScreen {
  private final MainActivity a;
  private final View root;
  private final String day;
  private final int token;

  PlanScreen(MainActivity a) {
    this.a = a;
    day = a.day.toString();
    token = a.token();
    root = a.layout(R.layout.screen_plan);
    Ui.text(root, R.id.plan_date, day);
    root.findViewById(R.id.plan_date)
        .setOnClickListener(
            v ->
                new DatePickerDialog(
                        a,
                        (p, y, m, d) -> {
                          a.day = LocalDate.of(y, m + 1, d);
                          a.show(R.id.nav_plan);
                        },
                        a.day.getYear(),
                        a.day.getMonthValue() - 1,
                        a.day.getDayOfMonth())
                    .show());
    root.findViewById(R.id.plan_add).setOnClickListener(v -> a.show(R.id.nav_library));
    a.setBusy(true);
    a.repo.io.execute(
        () -> {
          try {
            List<LibraryStore.Planned> plans = a.repo.library.plans(day);
            List<LibraryStore.Shopping> shopping = a.repo.library.shopping(day);
            a.runOnUiThread(
                () -> {
                  if (a.active(token)) {
                    a.setBusy(false);
                    render(plans, shopping);
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

  private void render(List<LibraryStore.Planned> plans, List<LibraryStore.Shopping> shopping) {
    LinearLayout rows = root.findViewById(R.id.plan_rows);
    double total = 0;
    if (plans.isEmpty()) label(rows, a.getString(R.string.plan_empty));
    for (LibraryStore.Planned plan : plans) {
      total += plan.recipe.kcal * plan.portions;
      View row = a.getLayoutInflater().inflate(R.layout.row_plan, rows, false);
      rows.addView(row);
      Ui.text(row, R.id.plan_name, plan.recipe.title);
      Ui.text(
          row,
          R.id.plan_info,
          a.getString(
              R.string.plan_detail,
              Ui.meal(a, plan.meal),
              plan.portions,
              plan.recipe.kcal * plan.portions));
      boolean future = LocalDate.parse(day).isAfter(LocalDate.now());
      Ui.text(
          row,
          R.id.consume,
          a.getString(
              plan.consumed
                  ? R.string.plan_eaten
                  : future ? R.string.plan_future : R.string.plan_consume));
      row.findViewById(R.id.consume).setEnabled(!plan.consumed && !future);
      row.findViewById(R.id.consume)
          .setOnClickListener(
              v -> {
                if (!Collections.disjoint(plan.recipe.food().allergens, a.profile.allergies)) {
                  Ui.error(a, R.string.allergen_warning);
                  return;
                }
                a.write(
                    () -> {
                      try {
                        a.repo.library.consume(plan.id);
                      } catch (Exception e) {
                        throw new IllegalStateException(e);
                      }
                    },
                    () -> a.show(R.id.nav_plan));
              });
      row.findViewById(R.id.remove_plan)
          .setOnClickListener(
              v ->
                  new MaterialAlertDialogBuilder(a)
                      .setMessage(R.string.plan_confirm_delete)
                      .setNegativeButton(R.string.cancel, null)
                      .setPositiveButton(
                          R.string.delete,
                          (d, w) ->
                              a.write(
                                  () -> a.repo.library.deletePlan(plan.id),
                                  () -> a.show(R.id.nav_plan)))
                      .show());
    }
    Ui.text(root, R.id.plan_total, a.getString(R.string.plan_total, total));
    LinearLayout items = root.findViewById(R.id.shopping_rows);
    if (shopping.isEmpty()) label(items, a.getString(R.string.shopping_empty));
    StringBuilder share =
        new StringBuilder(a.getString(R.string.shopping_title))
            .append(" · ")
            .append(day)
            .append('\n');
    for (LibraryStore.Shopping item : shopping) {
      MaterialCheckBox check = new MaterialCheckBox(a);
      String line = a.getString(R.string.ingredient_amount, item.name, item.grams);
      check.setText(line);
      check.setChecked(item.checked);
      items.addView(check);
      share.append(item.checked ? "✓ " : "• ").append(line).append('\n');
      check.setOnCheckedChangeListener(
          (v, checked) ->
              a.write(
                  () -> a.repo.library.checkShopping(day, item.id, checked),
                  () -> a.show(R.id.nav_plan)));
    }
    root.findViewById(R.id.shopping_share).setEnabled(!shopping.isEmpty());
    root.findViewById(R.id.shopping_share)
        .setOnClickListener(
            v -> {
              Intent send =
                  new Intent(Intent.ACTION_SEND)
                      .setType("text/plain")
                      .putExtra(Intent.EXTRA_TEXT, share.toString());
              a.startActivity(Intent.createChooser(send, a.getString(R.string.shopping_share)));
            });
  }

  private void label(LinearLayout parent, String value) {
    View row = a.getLayoutInflater().inflate(R.layout.row_weight, parent, false);
    Ui.text(row, R.id.row_weight, value);
    parent.addView(row);
  }
}
