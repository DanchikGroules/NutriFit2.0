package com.nutrifit.app;

import java.util.Arrays;
import java.util.List;

public final class Catalog {
    public static final String[] INGREDIENTS = {"chicken","pasta","yogurt","fish","peanut","rice","vegetables","oats","banana","egg","milk"};
    public static final String[] INGREDIENT_NAMES = {"Курица","Паста","Йогурт","Рыба","Арахис","Рис","Овощи","Овсянка","Банан","Яйцо","Молоко"};
    public static final String[] ALLERGENS = {"peanut","milk","egg","fish","gluten","soy","nuts","sesame","shellfish"};
    public static final String[] ALLERGEN_NAMES = {"Арахис","Молоко","Яйцо","Рыба","Глютен","Соя","Орехи","Кунжут","Ракообразные / моллюски"};
    public static final String[] RESTRICTIONS = {"vegetarian","vegan","lactose_free","gluten_free"};
    public static final String[] RESTRICTION_NAMES = {"Вегетарианское","Веганское","Без лактозы","Без глютена"};
    // Демонстрационные значения. Перед реальным использованием сверить состав и этикетки.
    public static final List<Food> FOODS = Arrays.asList(
        new Food("chicken_pasta","Паста с курицей",165,12,5,18,"chicken,pasta,vegetables","gluten","lactose_free"),
        new Food("chicken_rice","Курица с рисом",150,12,4,16.5,"chicken,rice,vegetables","","lactose_free,gluten_free"),
        new Food("yogurt_banana","Йогурт с бананом",100,4,2,16.5,"yogurt,banana,milk","milk","vegetarian,gluten_free"),
        new Food("fish_rice","Рыба с рисом",140,11,4,15,"fish,rice","fish","lactose_free,gluten_free"),
        new Food("rice_vegetables","Рис с овощами",110,3,2,20,"rice,vegetables","","vegetarian,vegan,lactose_free,gluten_free"),
        new Food("oats_banana","Овсянка на воде с бананом",115,3,3,19,"oats,banana","gluten","vegetarian,vegan,lactose_free"),
        new Food("omelet","Омлет с овощами",130,9,8,5.5,"egg,milk,vegetables","egg,milk","vegetarian,gluten_free"),
        new Food("peanut_yogurt","Йогурт с арахисом",180,8,10,14.5,"yogurt,milk,peanut","milk,peanut","vegetarian,gluten_free")
    );
}
