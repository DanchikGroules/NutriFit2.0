package com.nutrifit.app.model;

import java.util.ArrayList;
import java.util.List;

/** Nutrition is per recipe serving (or per 100 g for a standalone product). */
public final class Recipe {
  public final String id, title, category, allergens, tags;
  public final int minutes;
  public final double grams, kcal, protein, fat, carbs;
  public final boolean product;
  public final String searchText;
  public final java.util.Set<String> tagSet, allergenSet;
  public final List<Ingredient> ingredients;
  public final List<String> steps;

  public Recipe(
      String id,
      String title,
      String category,
      boolean product,
      int minutes,
      double grams,
      double kcal,
      double protein,
      double fat,
      double carbs,
      String tags,
      String allergens,
      List<Ingredient> ingredients,
      List<String> steps) {
    this.id = id;
    this.title = title;
    this.category = category;
    this.product = product;
    this.minutes = minutes;
    this.grams = grams;
    this.kcal = kcal;
    this.protein = protein;
    this.fat = fat;
    this.carbs = carbs;
    this.tags = tags;
    this.allergens = allergens;
    this.ingredients = java.util.Collections.unmodifiableList(new ArrayList<>(ingredients));
    this.steps = java.util.Collections.unmodifiableList(new ArrayList<>(steps));
    tagSet =
        java.util.Collections.unmodifiableSet(
            new java.util.HashSet<>(java.util.Arrays.asList(tags.split(","))));
    allergenSet =
        java.util.Collections.unmodifiableSet(
            new java.util.HashSet<>(java.util.Arrays.asList(allergens.split(","))));
    StringBuilder search = new StringBuilder(title);
    for (Ingredient ingredient : ingredients) search.append(' ').append(ingredient.name);
    searchText = searchKey(search.toString());
  }

  public static String searchKey(String text) {
    return java.text.Normalizer.normalize(
            text.trim().toLowerCase(java.util.Locale.ROOT), java.text.Normalizer.Form.NFD)
        .replaceAll("\\p{M}+", "")
        .replace('ł', 'l');
  }

  public Food food() {
    return new Food(
        id,
        title,
        kcal * 100 / grams,
        protein * 100 / grams,
        fat * 100 / grams,
        carbs * 100 / grams,
        "",
        allergens,
        tags);
  }

  public static final class Ingredient {
    public final String id, name;
    public final double grams;

    public Ingredient(String id, String name, double grams) {
      this.id = id;
      this.name = name;
      this.grams = grams;
    }
  }
}
