package com.nutrifit.app;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;

public class NutritionTest {
    @Test public void referenceCalculationAndMacroEnergyMatch() {
        Profile p=new Profile(); p.age=25; p.height=180; p.weight=80; p.goal=1; p.activity=0;
        NutritionCalculator.Target t=NutritionCalculator.calculate(p);
        assertEquals(1805,t.bmr,.001); assertEquals(2166,t.calories,.001);
        assertEquals(t.calories,t.protein*4+t.fat*9+t.carbs*4,.001);
        p.male=false; assertEquals(1639,NutritionCalculator.calculate(p).bmr,.001);
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsMinor() {
        Profile p=new Profile(); p.age=16; NutritionCalculator.calculate(p);
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsNan() {
        Profile p=new Profile(); p.weight=Double.NaN; NutritionCalculator.calculate(p);
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsWrongTargetDirection() {
        Profile p=new Profile(); p.goal=2; p.targetWeight=60; NutritionCalculator.calculate(p);
    }
    @Test public void allergensOverrideLikesAndPortionsFitBudget() {
        Profile p=new Profile(); p.likes.add("peanut"); p.allergies.add("peanut"); p.dislikes.add("fish");
        List<RecommendationEngine.Suggestion> list=RecommendationEngine.recommend(Catalog.FOODS,p,600);
        assertFalse(list.isEmpty());
        for(RecommendationEngine.Suggestion s:list) {
            assertFalse(s.food.allergens.contains("peanut")); assertFalse(s.food.ingredients.contains("fish"));
            assertTrue(s.calories<=600); assertTrue(s.grams>=100 && s.grams<=400);
        }
        for(Food f:Catalog.FOODS) if(f.allergens.contains("peanut")) assertFalse(RecommendationEngine.allowed(f,p));
    }
    @Test public void restrictionAndEmptyBudget() {
        Profile p=new Profile(); p.restrictions.add("vegan"); p.restrictions.add("gluten_free");
        List<RecommendationEngine.Suggestion> list=RecommendationEngine.recommend(Catalog.FOODS,p,600);
        assertEquals(1,list.size()); assertEquals("rice_vegetables",list.get(0).food.id);
        assertTrue(RecommendationEngine.recommend(Catalog.FOODS,p,0).isEmpty());
        assertTrue(RecommendationEngine.recommend(Catalog.FOODS,p,-50).isEmpty());
        assertTrue(RecommendationEngine.recommend(Catalog.FOODS,p,20).isEmpty());
        p.dislikes.add("rice"); assertTrue(RecommendationEngine.recommend(Catalog.FOODS,p,600).isEmpty());
    }
}
