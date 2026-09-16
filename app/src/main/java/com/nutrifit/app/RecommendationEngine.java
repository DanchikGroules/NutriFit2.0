package com.nutrifit.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class RecommendationEngine {
    public static final class Suggestion {
        public final Food food;
        public final int grams;
        public final double calories, score;
        Suggestion(Food f, int grams, double score) {
            food=f; this.grams=grams; calories=f.kcal * grams / 100; this.score=score;
        }
    }
    public static boolean allowed(Food f, Profile p) {
        return Collections.disjoint(f.allergens,p.allergies)
                && Collections.disjoint(f.ingredients,p.dislikes)
                && f.tags.containsAll(p.restrictions);
    }
    public static List<Suggestion> recommend(List<Food> foods, Profile p, double remaining) {
        List<Suggestion> result=new ArrayList<>();
        if (!Double.isFinite(remaining) || remaining <= 0) return result;
        double budget=Math.min(remaining,600); // одна порция, а не весь остаток дня
        for (Food food : foods) {
            if (!allowed(food,p) || food.kcal <= 0) continue;
            int grams=Math.min(400, (int)Math.floor(budget / food.kcal * 10) * 10);
            if (grams < 100) continue;
            long liked=food.ingredients.stream().filter(p.likes::contains).count();
            double score=liked * 100 - Math.abs(budget-food.kcal * grams/100) / budget * 30;
            result.add(new Suggestion(food,grams,score));
        }
        result.sort(Comparator.comparingDouble((Suggestion s)->s.score).reversed());
        return result.subList(0,Math.min(3,result.size()));
    }
}
