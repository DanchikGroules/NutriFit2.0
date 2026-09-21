package com.nutrifit.app.ui;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.nutrifit.app.R;
import com.nutrifit.app.data.AppDatabase;
import com.nutrifit.app.model.Profile;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

final class WeightScreen {
  WeightScreen(MainActivity a) {
    View root = a.layout(R.layout.screen_weight);
    Ui.text(root, R.id.weight_goal, a.getString(R.string.weight_goal, a.profile.targetWeight));
    Ui.text(root, R.id.weight_today, Double.toString(a.profile.weight));
    root.findViewById(R.id.save_weight)
        .setOnClickListener(
            v -> {
              try {
                double kg = Ui.number(root, R.id.weight_today);
                if (!Double.isFinite(kg) || kg < 35 || kg > 250) {
                  Ui.error(a, R.string.weight_error);
                  return;
                }
                Profile updated = a.repo.profiles.load();
                updated.weight = kg;
                boolean reached =
                    updated.goal == 0 && kg <= updated.targetWeight
                        || updated.goal == 2 && kg >= updated.targetWeight;
                if (reached) updated.goal = 1;
                a.write(
                    () -> {
                      a.repo.db.saveWeight(LocalDate.now().toString(), kg);
                      a.repo.profiles.save(updated);
                    },
                    () -> {
                      if (reached)
                        Toast.makeText(a, R.string.goal_reached, Toast.LENGTH_LONG).show();
                      else a.saved();
                      a.show(R.id.nav_progress);
                    });
              } catch (NumberFormatException e) {
                Ui.error(a, R.string.number_error);
              }
            });
    int token = a.token();
    a.setBusy(true);
    a.repo.io.execute(
        () -> {
          try {
            List<AppDatabase.Weight> weights = a.repo.db.weights();
            a.runOnUiThread(
                () -> {
                  if (!a.active(token)) return;
                  a.setBusy(false);
                  LinearLayout history = root.findViewById(R.id.weight_history);
                  if (weights.isEmpty()) {
                    Ui.text(root, R.id.weight_change, a.getString(R.string.empty_weight));
                    return;
                  }
                  Ui.text(
                      root,
                      R.id.weight_change,
                      a.getString(
                          R.string.weight_change,
                          weights.get(weights.size() - 1).kg - weights.get(0).kg));
                  WeightChartView chart = new WeightChartView(a, weights);
                  chart.setContentDescription(a.getString(R.string.chart_description));
                  ((LinearLayout) root.findViewById(R.id.chart_holder))
                      .addView(
                          chart,
                          new LinearLayout.LayoutParams(
                              -1, (int) (240 * a.getResources().getDisplayMetrics().density)));
                  for (int i = weights.size() - 1; i >= 0; i--) {
                    AppDatabase.Weight weight = weights.get(i);
                    View row = a.getLayoutInflater().inflate(R.layout.row_weight, history, false);
                    history.addView(row);
                    String date =
                        LocalDate.parse(weight.day)
                            .format(
                                DateTimeFormatter.ofPattern(
                                    "d MMM yyyy",
                                    a.getResources().getConfiguration().getLocales().get(0)));
                    Ui.text(
                        row, R.id.row_weight, a.getString(R.string.weight_row, date, weight.kg));
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
}
