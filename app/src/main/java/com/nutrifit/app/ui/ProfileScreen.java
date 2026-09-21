package com.nutrifit.app.ui;

import android.view.View;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nutrifit.app.R;
import com.nutrifit.app.data.Catalog;
import com.nutrifit.app.domain.NutritionCalculator;
import com.nutrifit.app.model.Profile;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;

final class ProfileScreen {
  final Profile draft;

  ProfileScreen(MainActivity a, Profile restored) {
    View root = a.layout(R.layout.screen_profile);
    root.findViewById(R.id.language).setOnClickListener(v -> LanguageSettings.show(a));
    draft =
        restored != null ? restored : (a.profile == null ? new Profile() : a.repo.profiles.load());
    if (draft.name.isEmpty()) draft.name = a.repo.account.name();
    Ui.text(
        root,
        R.id.account,
        a.repo.isDemo
            ? a.getString(R.string.demo_account)
            : a.getString(R.string.account_info, a.repo.account.email()));
    Ui.text(root, R.id.profile_name, draft.name);
    Ui.text(root, R.id.age, Integer.toString(draft.age));
    Ui.text(root, R.id.height, Double.toString(draft.height));
    Ui.text(root, R.id.weight, Double.toString(draft.weight));
    Ui.text(root, R.id.target, Double.toString(draft.targetWeight));
    Ui.spinner(root, R.id.sex, R.array.sex, draft.male ? 0 : 1);
    Ui.spinner(root, R.id.activity, R.array.activity, draft.activity);
    Ui.spinner(root, R.id.goal, R.array.goals, draft.goal);
    choices(
        a, root, R.id.likes, R.string.likes, Catalog.INGREDIENTS, R.array.ingredients, draft.likes);
    choices(
        a,
        root,
        R.id.dislikes,
        R.string.dislikes,
        Catalog.INGREDIENTS,
        R.array.ingredients,
        draft.dislikes);
    choices(
        a,
        root,
        R.id.allergies,
        R.string.allergies,
        Catalog.ALLERGENS,
        R.array.allergens,
        draft.allergies);
    choices(
        a,
        root,
        R.id.restrictions,
        R.string.restrictions,
        Catalog.RESTRICTIONS,
        R.array.restriction_names,
        draft.restrictions);
    root.findViewById(R.id.save_profile)
        .setOnClickListener(
            v -> {
              try {
                draft.name = Ui.value(root, R.id.profile_name);
                if (draft.name.isEmpty()) {
                  Ui.error(a, R.string.name_error);
                  return;
                }
                double age = Ui.number(root, R.id.age);
                if (age != Math.floor(age)) {
                  Ui.error(a, R.string.age_error);
                  return;
                }
                draft.age = (int) age;
                draft.height = Ui.number(root, R.id.height);
                draft.weight = Ui.number(root, R.id.weight);
                draft.targetWeight = Ui.number(root, R.id.target);
                draft.male = Ui.selected(root, R.id.sex) == 0;
                draft.activity = Ui.selected(root, R.id.activity);
                draft.goal = Ui.selected(root, R.id.goal);
                try {
                  NutritionCalculator.calculate(draft);
                } catch (IllegalArgumentException e) {
                  Ui.error(
                      a,
                      "goal".equals(e.getMessage()) ? R.string.goal_error : R.string.range_error);
                  return;
                }
                if (!Collections.disjoint(draft.likes, draft.dislikes)) {
                  Ui.error(a, R.string.preference_error);
                  return;
                }
                a.write(
                    () -> {
                      a.repo.db.saveWeight(LocalDate.now().toString(), draft.weight);
                      a.repo.profiles.save(draft);
                    },
                    () -> {
                      a.saved();
                      a.show(R.id.nav_diary);
                    });
              } catch (NumberFormatException e) {
                Ui.error(a, R.string.number_error);
              }
            });
    root.findViewById(R.id.logout)
        .setOnClickListener(
            v ->
                new MaterialAlertDialogBuilder(a)
                    .setMessage(a.repo.isDemo ? R.string.demo_exit_note : R.string.logout_question)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(
                        R.string.logout, (d, w) -> a.write(a.repo::signOut, a::openAuth))
                    .show());
    root.findViewById(R.id.change_password)
        .setOnClickListener(v -> AccountDialogs.change(a, false));
    root.findViewById(R.id.recovery_create).setOnClickListener(v -> AccountDialogs.change(a, true));
    root.findViewById(R.id.profile_plus).setOnClickListener(v -> a.show(R.id.nav_premium));
    if (a.repo.isDemo) {
      Ui.text(root, R.id.logout, a.getString(R.string.demo_exit));
      for (int id :
          new int[] {
            R.id.security_heading, R.id.security_note, R.id.change_password, R.id.recovery_create
          }) root.findViewById(id).setVisibility(View.GONE);
    }
  }

  private void choices(
      MainActivity a, View root, int id, int title, String[] keys, int labels, Set<String> values) {
    root.findViewById(id)
        .setOnClickListener(
            v -> {
              boolean[] checked = new boolean[keys.length];
              for (int i = 0; i < keys.length; i++) checked[i] = values.contains(keys[i]);
              new MaterialAlertDialogBuilder(a)
                  .setTitle(title)
                  .setMultiChoiceItems(
                      a.getResources().getStringArray(labels),
                      checked,
                      (dialog, i, on) -> checked[i] = on)
                  .setNegativeButton(R.string.cancel, null)
                  .setPositiveButton(
                      R.string.save,
                      (d, w) -> {
                        values.clear();
                        for (int i = 0; i < keys.length; i++) if (checked[i]) values.add(keys[i]);
                      })
                  .show();
            });
  }
}
