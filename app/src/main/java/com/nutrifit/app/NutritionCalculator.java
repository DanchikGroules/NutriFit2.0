package com.nutrifit.app;

public final class NutritionCalculator {
    private static final double[] ACTIVITY = {1.2, 1.375, 1.55, 1.725};
    public static final class Target {
        public final double bmr, calories, protein, fat, carbs;
        Target(double bmr, double calories) {
            this.bmr = bmr; this.calories = calories;
            protein = calories * .20 / 4;
            fat = calories * .30 / 9;
            carbs = calories * .50 / 4;
        }
    }
    public static Target calculate(Profile p) {
        if (p.age < 18 || p.age > 100 || !range(p.height,120,230)
                || !range(p.weight,35,250) || !range(p.targetWeight,35,250)
                || p.activity < 0 || p.activity >= ACTIVITY.length || p.goal < 0 || p.goal > 2)
            throw new IllegalArgumentException("Проверьте данные: возраст 18–100, рост 120–230 см, вес 35–250 кг.");
        if (p.goal == 0 && p.targetWeight >= p.weight || p.goal == 2 && p.targetWeight <= p.weight)
            throw new IllegalArgumentException("Целевой вес должен соответствовать выбранной цели.");
        double bmr = 10 * p.weight + 6.25 * p.height - 5 * p.age + (p.male ? 5 : -161);
        // Учебные коэффициенты целей, не индивидуальное медицинское назначение.
        double factor = p.goal == 0 ? .90 : p.goal == 2 ? 1.10 : 1;
        return new Target(bmr, bmr * ACTIVITY[p.activity] * factor);
    }
    private static boolean range(double x, double min, double max) {
        return Double.isFinite(x) && x >= min && x <= max;
    }
}
