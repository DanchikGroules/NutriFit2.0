package com.nutrifit.app;

import static org.junit.Assert.*;

import com.nutrifit.app.domain.RecipeFilter;
import com.nutrifit.app.model.Recipe;
import java.nio.file.*;
import java.util.*;
import org.json.*;
import org.junit.Test;

public class CatalogTest {
  private JSONArray asset(String name) throws Exception {
    return new JSONArray(
        new String(
            Files.readAllBytes(java.nio.file.Paths.get(System.getProperty("catalogPath"), name)),
            java.nio.charset.StandardCharsets.UTF_8));
  }

  private List<Recipe> all() throws Exception {
    List<Recipe> all = new ArrayList<>();
    for (String file : new String[] {"recipes.json", "products.json"}) {
      JSONArray a = asset(file);
      for (int i = 0; i < a.length(); i++)
        all.add(new Recipe(a.getJSONObject(i), file.equals("products.json")));
    }
    return all;
  }

  @Test
  public void catalogHas120DistinctRecipesAnd100Products() throws Exception {
    JSONArray recipes = asset("recipes.json"), products = asset("products.json");
    assertEquals(120, recipes.length());
    assertEquals(100, products.length());
    Set<String> ids = new HashSet<>(), names = new HashSet<>();
    Map<String, Integer> categories = new HashMap<>();
    for (Recipe r : all()) {
      assertTrue(ids.add(r.id));
      assertTrue(names.add(r.title));
      assertTrue(r.grams > 0);
      assertTrue(r.kcal >= 0);
      if (!r.product) {
        assertTrue(r.steps.size() >= 3);
        assertTrue(r.ingredients.size() >= 3);
        categories.merge(r.category, 1, Integer::sum);
      }
    }
    assertEquals(12, categories.size());
    for (int count : categories.values()) assertEquals(10, count);
  }

  @Test
  public void recipeNutritionAndAllergensMatchIngredients() throws Exception {
    Map<String, JSONObject> products = new HashMap<>();
    JSONArray input = asset("products.json");
    for (int i = 0; i < input.length(); i++)
      products.put(input.getJSONObject(i).getString("id"), input.getJSONObject(i));
    for (Recipe r : all())
      if (!r.product) {
        double kcal = 0;
        Set<String> allergens = new HashSet<>();
        for (Recipe.Ingredient ingredient : r.ingredients) {
          JSONObject p = products.get(ingredient.id);
          assertNotNull(p);
          assertTrue(ingredient.grams > 0);
          kcal += p.getDouble("kcal") * ingredient.grams / 100;
          for (String x : p.getString("allergens").split(",")) if (!x.isEmpty()) allergens.add(x);
        }
        assertEquals(kcal, r.kcal, .051);
        Set<String> actual = new HashSet<>(Arrays.asList(r.allergens.split(",")));
        actual.remove("");
        assertEquals(allergens, actual);
      }
  }

  @Test
  public void searchFindsIngredientsAndFiltersProductsAndFavorites() throws Exception {
    List<Recipe> all = all();
    Set<String> none = Collections.emptySet();
    List<Recipe> found = RecipeFilter.apply(all, "  ТоМаТ  ", "", "", 0, none, none);
    assertFalse(found.isEmpty());
    for (Recipe r : found) assertFalse(r.product);
    assertEquals(100, RecipeFilter.apply(all, "", "", "", 1, none, none).size());
    assertEquals(1, RecipeFilter.apply(all, "", "", "", 2, Set.of("recipe_001"), none).size());
    for (Recipe r : RecipeFilter.apply(all, "", "", "vegan", 0, none, Set.of("gluten"))) {
      assertTrue(r.tags.contains("vegan"));
      assertFalse(r.allergens.contains("gluten"));
    }
    assertTrue(RecipeFilter.apply(all, "does-not-exist", "", "", 0, none, none).isEmpty());
  }

  @Test
  public void videoSourcesAreHttpsAndNeverPremium() throws Exception {
    JSONArray lessons = asset("lessons.json");
    int videos = 0;
    for (int i = 0; i < lessons.length(); i++) {
      JSONObject lesson = lessons.getJSONObject(i);
      if (lesson.has("url")) {
        videos++;
        assertTrue(lesson.getString("url").startsWith("https://"));
        assertFalse(lesson.getBoolean("premium"));
        assertFalse(lesson.getString("author").isEmpty());
      }
    }
    assertEquals(8, videos);
  }
}
