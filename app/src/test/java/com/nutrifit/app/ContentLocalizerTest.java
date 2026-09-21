package com.nutrifit.app;

import static org.junit.Assert.*;

import com.nutrifit.app.domain.ContentLocalizer;
import com.nutrifit.app.domain.RecipeFilter;
import com.nutrifit.app.model.Recipe;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.json.*;
import org.junit.Test;

public class ContentLocalizerTest {
  private String asset(String name) throws Exception {
    return new String(
        Files.readAllBytes(Paths.get(System.getProperty("catalogPath"), name)),
        StandardCharsets.UTF_8);
  }

  private ContentLocalizer localizer(String language) throws Exception {
    return new ContentLocalizer(
        language,
        new JSONObject(asset("translations/en.json")),
        new JSONObject(asset("translations/pl.json")));
  }

  private void translated(String value) {
    assertFalse("Untranslated content: " + value, value.matches("(?s).*[А-Яа-яЁё].*"));
    assertFalse(value.isEmpty());
  }

  @Test
  public void allRecipesProductsAndLessonsAreTranslatedWithoutChangingData() throws Exception {
    for (String language : new String[] {"en", "pl"}) {
      ContentLocalizer localizer = localizer(language);
      for (String file : new String[] {"recipes.json", "products.json", "lessons.json"}) {
        JSONArray items = new JSONArray(asset(file));
        for (int i = 0; i < items.length(); i++) {
          JSONObject original = items.getJSONObject(i), target = localizer.payload(original);
          assertEquals(original.getString("id"), target.getString("id"));
          translated(target.getString("title"));
          for (String key : new String[] {"author", "body"})
            if (target.has(key)) translated(target.getString(key));
          for (String key :
              new String[] {
                "kcal",
                "protein",
                "fat",
                "carbs",
                "grams",
                "minutes",
                "category",
                "tags",
                "allergens",
                "premium",
                "url"
              })
            if (original.has(key)) {
              if (original.get(key) instanceof Number)
                assertEquals(original.getDouble(key), target.getDouble(key), 0);
              else assertEquals(original.get(key), target.get(key));
            }
          JSONArray steps = target.optJSONArray("steps");
          if (steps != null) {
            assertEquals(original.getJSONArray("steps").length(), steps.length());
            for (int n = 0; n < steps.length(); n++) translated(steps.getString(n));
          }
          JSONArray ingredients = target.optJSONArray("ingredients");
          if (ingredients != null)
            for (int n = 0; n < ingredients.length(); n++) {
              JSONObject item = ingredients.getJSONObject(n),
                  before = original.getJSONArray("ingredients").getJSONObject(n);
              translated(item.getString("name"));
              assertEquals(before.getString("id"), item.getString("id"));
              assertEquals(before.getDouble("grams"), item.getDouble("grams"), 0);
            }
          // The source JSON used for database snapshots remains unmodified.
          assertEquals(items.getJSONObject(i).getString("title"), original.getString("title"));
        }
      }
    }
  }

  @Test
  public void historicalDiaryNamesTranslateInEveryDirection() throws Exception {
    JSONArray recipes = new JSONArray(asset("recipes.json"));
    for (int i = 0; i < recipes.length(); i++) {
      String original = recipes.getJSONObject(i).getString("title");
      for (String from : new String[] {"ru", "en", "pl"})
        for (String to : new String[] {"ru", "en", "pl"})
          assertEquals(
              localizer(to).text(original), localizer(to).snapshot(localizer(from).text(original)));
    }
    assertEquals("My own dish", localizer("pl").snapshot("My own dish"));
  }

  @Test
  public void localizedSearchHandlesPolishLettersAndIngredients() throws Exception {
    JSONArray input = new JSONArray(asset("recipes.json"));
    List<Recipe> recipes = new ArrayList<>();
    ContentLocalizer pl = localizer("pl");
    for (int i = 0; i < input.length(); i++)
      recipes.add(new Recipe(pl.payload(input.getJSONObject(i)), false));
    Set<String> empty = Collections.emptySet();
    List<Recipe> salmon =
        RecipeFilter.apply(recipes, "losos", "", "", 0, empty, empty, Locale.forLanguageTag("pl"));
    assertEquals(5, salmon.size());
    assertFalse(
        RecipeFilter.apply(
                recipes, "platki owsiane", "", "", 0, empty, empty, Locale.forLanguageTag("pl"))
            .isEmpty());
  }

  @Test
  public void payloadTranslationDoesNotMutateOriginal() throws Exception {
    JSONObject original = new JSONArray(asset("recipes.json")).getJSONObject(0);
    String before = original.toString();
    JSONObject target = localizer("en").payload(original);
    assertNotEquals(original.getString("title"), target.getString("title"));
    assertEquals(before, original.toString());
  }
}
