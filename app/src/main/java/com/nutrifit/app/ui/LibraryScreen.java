package com.nutrifit.app.ui;

import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.*;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.nutrifit.app.R;
import com.nutrifit.app.domain.RecipeFilter;
import com.nutrifit.app.model.Recipe;
import java.util.*;

final class LibraryScreen {
  static final String[] CATEGORIES = {
    "",
    "breakfast",
    "eggs",
    "salad",
    "soup",
    "pasta",
    "bowl",
    "poultry",
    "fish",
    "plant",
    "snack",
    "drink",
    "dessert"
  };
  static final String[] DIETS = {"", "vegetarian", "vegan", "lactose_free", "gluten_free"};
  private final MainActivity a;
  private final View root;
  private final int token;
  private int request;
  private final Adapter adapter = new Adapter();
  private final android.os.Handler handler =
      new android.os.Handler(android.os.Looper.getMainLooper());

  LibraryScreen(MainActivity a) {
    this.a = a;
    token = a.token();
    root = a.layout(R.layout.screen_library);
    Ui.spinner(root, R.id.category, R.array.categories, 0);
    Ui.spinner(root, R.id.diet, R.array.diets, 0);
    RecyclerView list = root.findViewById(R.id.recipe_list);
    int columns =
        a.getResources().getConfiguration().screenWidthDp >= 380
                && a.getResources().getConfiguration().fontScale <= 1.2
            ? 2
            : 1;
    list.setLayoutManager(new GridLayoutManager(a, columns));
    list.setAdapter(adapter);
    ((EditText) root.findViewById(R.id.search))
        .addTextChangedListener(
            new TextWatcher() {
              public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

              public void onTextChanged(CharSequence s, int start, int before, int count) {
                schedule();
              }

              public void afterTextChanged(Editable e) {}
            });
    AdapterView.OnItemSelectedListener listener =
        new AdapterView.OnItemSelectedListener() {
          public void onItemSelected(AdapterView<?> p, View v, int position, long id) {
            schedule();
          }

          public void onNothingSelected(AdapterView<?> p) {}
        };
    ((Spinner) root.findViewById(R.id.category)).setOnItemSelectedListener(listener);
    ((Spinner) root.findViewById(R.id.diet)).setOnItemSelectedListener(listener);
    for (int id : new int[] {R.id.filter_recipes, R.id.filter_products, R.id.filter_favorites})
      ((Chip) root.findViewById(id))
          .setOnCheckedChangeListener(
              (v, on) -> {
                if (on) schedule();
              });
    ((CheckBox) root.findViewById(R.id.safe_filter))
        .setOnCheckedChangeListener((v, on) -> schedule());
    schedule();
  }

  void schedule() {
    handler.removeCallbacksAndMessages(null);
    int serial = ++request;
    handler.postDelayed(
        () -> {
          if (serial == request && a.active(token)) load(serial);
        },
        180);
  }

  private void load(int serial) {
    Locale locale = a.getResources().getConfiguration().getLocales().get(0);
    String query = Ui.value(root, R.id.search),
        category = CATEGORIES[Ui.selected(root, R.id.category)],
        diet = DIETS[Ui.selected(root, R.id.diet)];
    int mode =
        ((Chip) root.findViewById(R.id.filter_products)).isChecked()
            ? 1
            : ((Chip) root.findViewById(R.id.filter_favorites)).isChecked() ? 2 : 0;
    Set<String> allergens =
        ((CheckBox) root.findViewById(R.id.safe_filter)).isChecked()
            ? new HashSet<>(a.profile.allergies)
            : Collections.emptySet();
    a.repo.io.execute(
        () -> {
          try {
            Set<String> favorites = a.repo.library.favorites();
            List<Recipe> rows =
                RecipeFilter.apply(
                    a.repo.library.all(),
                    query,
                    category,
                    diet,
                    mode,
                    favorites,
                    allergens,
                    locale);
            a.runOnUiThread(
                () -> {
                  if (!a.active(token) || serial != request) return;
                  adapter.update(rows, favorites);
                  Ui.text(
                      root, R.id.result_count, a.getString(R.string.library_count, rows.size()));
                  root.findViewById(R.id.empty)
                      .setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
                });
          } catch (Exception e) {
            a.runOnUiThread(
                () -> {
                  if (a.active(token)) Ui.error(a, R.string.storage_error);
                });
          }
        });
  }

  static String category(MainActivity a, Recipe recipe) {
    int index = Arrays.asList(CATEGORIES).indexOf(recipe.category);
    return index < 0
        ? a.getString(R.string.product_tag)
        : a.getResources().getStringArray(R.array.categories)[index];
  }

  private final class Adapter extends RecyclerView.Adapter<Holder> {
    List<Recipe> rows = Collections.emptyList();
    Set<String> favorites = Collections.emptySet();

    void update(List<Recipe> next, Set<String> stars) {
      DiffUtil.DiffResult changes =
          DiffUtil.calculateDiff(
              new DiffUtil.Callback() {
                public int getOldListSize() {
                  return rows.size();
                }

                public int getNewListSize() {
                  return next.size();
                }

                public boolean areItemsTheSame(int old, int fresh) {
                  return rows.get(old).id.equals(next.get(fresh).id);
                }

                public boolean areContentsTheSame(int old, int fresh) {
                  String id = rows.get(old).id;
                  return favorites.contains(id) == stars.contains(id);
                }
              });
      rows = next;
      favorites = stars;
      changes.dispatchUpdatesTo(this);
    }

    public Holder onCreateViewHolder(ViewGroup parent, int type) {
      return new Holder(a.getLayoutInflater().inflate(R.layout.row_recipe, parent, false));
    }

    public int getItemCount() {
      return rows.size();
    }

    public void onBindViewHolder(Holder holder, int position) {
      Recipe recipe = rows.get(position);
      View row = holder.itemView;
      Ui.text(row, R.id.recipe_name, recipe.title);
      Ui.text(
          row,
          R.id.recipe_category,
          category(a, recipe).toUpperCase(a.getResources().getConfiguration().getLocales().get(0)));
      Ui.text(
          row,
          R.id.recipe_meta,
          recipe.product
              ? a.getString(R.string.product_meta, recipe.kcal)
              : a.getString(R.string.recipe_meta, recipe.minutes, recipe.kcal));
      int[] colors = {R.color.peach, R.color.mint, R.color.lilac};
      ((MaterialCardView) row)
          .setCardBackgroundColor(a.getColor(colors[Math.floorMod(recipe.category.hashCode(), 3)]));
      boolean favorite = favorites.contains(recipe.id);
      Ui.text(
          row,
          R.id.favorite,
          a.getString(favorite ? R.string.favorite_remove : R.string.favorite_add));
      row.setOnClickListener(v -> a.openRecipe(recipe.id));
      row.findViewById(R.id.favorite)
          .setOnClickListener(
              v ->
                  a.write(
                      () -> a.repo.library.favorite(recipe.id, !favorite),
                      LibraryScreen.this::schedule));
    }
  }

  private static final class Holder extends RecyclerView.ViewHolder {
    Holder(View view) {
      super(view);
    }
  }
}
