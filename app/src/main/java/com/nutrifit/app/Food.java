package com.nutrifit.app;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Food {
    public final String id, name;
    public final double kcal, protein, fat, carbs; // per 100g, prepared food
    public final Set<String> ingredients, allergens, tags;
    public Food(String id, String name, double kcal, double protein, double fat, double carbs,
                String ingredients, String allergens, String tags) {
        this.id=id; this.name=name; this.kcal=kcal; this.protein=protein; this.fat=fat; this.carbs=carbs;
        this.ingredients=set(ingredients); this.allergens=set(allergens); this.tags=set(tags);
    }
    private static Set<String> set(String csv) {
        return csv.isEmpty() ? new HashSet<>() : new HashSet<>(Arrays.asList(csv.split(",")));
    }
    @Override public String toString() { return name; }
}
