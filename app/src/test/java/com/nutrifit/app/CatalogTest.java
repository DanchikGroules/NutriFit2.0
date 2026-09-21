package com.nutrifit.app;

import static org.junit.Assert.*;

import com.nutrifit.app.domain.RecipeFilter;
import com.nutrifit.app.model.Recipe;
import java.util.*;
import org.junit.Test;

public class CatalogTest {
  private Recipe recipe() {
    return new Recipe(
        "recipe_001",
        "Owsianka",
        "breakfast",
        false,
        10,
        200,
        300,
        10,
        5,
        45,
        "vegan",
        "gluten",
        List.of(new Recipe.Ingredient("oats", "Płatki owsiane", 50)),
        List.of("Gotuj"));
  }

  @Test
  public void searchNormalizesPolishAndFindsIngredients() {
    Set<String> empty = Collections.emptySet();
    assertEquals(
        1, RecipeFilter.apply(List.of(recipe()), "platki owsiane", "", "", 0, empty, empty).size());
    assertTrue(
        RecipeFilter.apply(List.of(recipe()), "", "", "", 0, empty, Set.of("gluten")).isEmpty());
    assertEquals(
        1,
        RecipeFilter.apply(List.of(recipe()), "", "", "", 2, Set.of("recipe_001"), empty).size());
  }

  @Test
  public void servingConvertsTo100Grams() {
    assertEquals(150, recipe().food().kcal, 0);
    assertEquals(5, recipe().food().protein, 0);
  }
}
