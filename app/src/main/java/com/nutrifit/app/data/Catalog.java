package com.nutrifit.app.data;

import com.nutrifit.app.model.Food;
import java.util.Arrays;
import java.util.List;

public final class Catalog {
  public static final String[] INGREDIENTS = {
    "chicken",
    "pasta",
    "yogurt",
    "fish",
    "peanut",
    "rice",
    "vegetables",
    "oats",
    "banana",
    "egg",
    "milk"
  };
  public static final String[] ALLERGENS = {
    "peanut", "milk", "egg", "fish", "gluten", "soy", "nuts", "sesame", "shellfish"
  };
  public static final String[] RESTRICTIONS = {
    "vegetarian", "vegan", "lactose_free", "gluten_free"
  };
  // Демонстрационные значения. Перед реальным использованием сверить состав и этикетки.
  public static final List<Food> FOODS =
      Arrays.asList(
          new Food(
              "chicken_pasta",
              "chicken_pasta",
              165,
              12,
              5,
              18,
              "chicken,pasta,vegetables",
              "gluten",
              "lactose_free"),
          new Food(
              "chicken_rice",
              "chicken_rice",
              150,
              12,
              4,
              16.5,
              "chicken,rice,vegetables",
              "",
              "lactose_free,gluten_free"),
          new Food(
              "yogurt_banana",
              "yogurt_banana",
              100,
              4,
              2,
              16.5,
              "yogurt,banana,milk",
              "milk",
              "vegetarian,gluten_free"),
          new Food(
              "fish_rice",
              "fish_rice",
              140,
              11,
              4,
              15,
              "fish,rice",
              "fish",
              "lactose_free,gluten_free"),
          new Food(
              "rice_vegetables",
              "rice_vegetables",
              110,
              3,
              2,
              20,
              "rice,vegetables",
              "",
              "vegetarian,vegan,lactose_free,gluten_free"),
          new Food(
              "oats_banana",
              "oats_banana",
              115,
              3,
              3,
              19,
              "oats,banana",
              "gluten",
              "vegetarian,vegan,lactose_free"),
          new Food(
              "omelet",
              "omelet",
              130,
              9,
              8,
              5.5,
              "egg,milk,vegetables",
              "egg,milk",
              "vegetarian,gluten_free"),
          new Food(
              "peanut_yogurt",
              "peanut_yogurt",
              180,
              8,
              10,
              14.5,
              "yogurt,milk,peanut",
              "milk,peanut",
              "vegetarian,gluten_free"));
}
