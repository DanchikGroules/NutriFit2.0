package com.nutrifit.app.domain;

import com.nutrifit.app.model.Recipe;
import java.util.*;

public final class RecipeFilter {
  public static List<Recipe> apply(
      List<Recipe> source,
      String query,
      String category,
      String diet,
      int mode,
      Set<String> favorites,
      Set<String> excludedAllergens) {
    return apply(
        source,
        query,
        category,
        diet,
        mode,
        favorites,
        excludedAllergens,
        Locale.forLanguageTag("ru"));
  }

  public static List<Recipe> apply(
      List<Recipe> source,
      String query,
      String category,
      String diet,
      int mode,
      Set<String> favorites,
      Set<String> excludedAllergens,
      Locale locale) {
    String search = Recipe.searchKey(query);
    List<Recipe> result = new ArrayList<>();
    for (Recipe recipe : source) {
      if (mode == 0 && recipe.product
          || mode == 1 && !recipe.product
          || mode == 2 && !favorites.contains(recipe.id)) continue;
      if (!recipe.product && !category.isEmpty() && !recipe.category.equals(category)) continue;
      if (!diet.isEmpty() && !recipe.tagSet.contains(diet)) continue;
      if (!Collections.disjoint(recipe.allergenSet, excludedAllergens)) continue;
      if (!recipe.searchText.contains(search)) continue;
      result.add(recipe);
    }
    java.text.Collator collator = java.text.Collator.getInstance(locale);
    result.sort((a, b) -> collator.compare(a.title, b.title));
    return result;
  }
}
