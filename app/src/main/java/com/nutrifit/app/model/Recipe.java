package com.nutrifit.app.model;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Nutrition is per recipe serving (or per 100 g for a standalone product). */
public final class Recipe {
  public final String id, title, category, allergens, tags;
  public final int minutes;
  public final double grams, kcal, protein, fat, carbs;
  public final boolean product;
  public final String searchText;
  public final java.util.Set<String> tagSet, allergenSet;
  public final List<Ingredient> ingredients = new ArrayList<>();
  public final List<String> steps = new ArrayList<>();

  public Recipe(JSONObject data, boolean product) throws Exception {
    this.product = product;
    id = (product ? "product_" : "") + data.getString("id");
    title = data.getString("title");
    category = product ? "product" : data.getString("category");
    minutes = data.optInt("minutes");
    grams = product ? 100 : data.getDouble("grams");
    kcal = data.getDouble("kcal");
    protein = data.getDouble("protein");
    fat = data.getDouble("fat");
    carbs = data.getDouble("carbs");
    allergens = data.optString("allergens");
    String kind = data.optString("kind");
    tags = product ? productTags(kind, allergens) : data.optString("tags");
    JSONArray list = data.optJSONArray("ingredients");
    if (list != null)
      for (int i = 0; i < list.length(); i++) {
        JSONObject item = list.getJSONObject(i);
        ingredients.add(
            new Ingredient(item.getString("id"), item.getString("name"), item.getDouble("grams")));
      }
    list = data.optJSONArray("steps");
    if (list != null) for (int i = 0; i < list.length(); i++) steps.add(list.getString(i));
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

  private static String productTags(String kind, String allergens) {
    List<String> tags = new ArrayList<>();
    if (!kind.equals("meat") && !kind.equals("fish")) tags.add("vegetarian");
    if (kind.equals("plant")) tags.add("vegan");
    if (!allergens.contains("milk")) tags.add("lactose_free");
    if (!allergens.contains("gluten")) tags.add("gluten_free");
    return String.join(",", tags);
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
