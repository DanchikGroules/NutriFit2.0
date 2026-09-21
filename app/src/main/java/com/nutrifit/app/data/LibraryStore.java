package com.nutrifit.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.nutrifit.app.model.Recipe;
import java.time.LocalDate;
import java.util.*;

/** All methods run on LocalRepository.io. Catalog is seeded transactionally and queried locally. */
public final class LibraryStore {
  private final AppDatabase helper;
  private final Context context;
  private List<Recipe> cache;
  private Map<String, Recipe> byId = Collections.emptyMap();
  private String language = "ru";
  private volatile com.nutrifit.app.domain.ContentLocalizer localizer;

  public void initialize(String tag) throws Exception {
    String next = ContentTranslations.supported(tag);
    if (!language.equals(next)) {
      language = next;
      cache = null;
    }
    initialize();
  }

  public String displayName(String stored) {
    com.nutrifit.app.domain.ContentLocalizer current = localizer;
    return current == null ? stored : current.snapshot(stored);
  }

  public LibraryStore(Context context, AppDatabase db) {
    this.context = context;
    helper = db;
  }

  public static void createSchema(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE favorites(recipe_id TEXT PRIMARY KEY REFERENCES catalog_items(id) ON DELETE"
            + " CASCADE)");
    db.execSQL(
        "CREATE TABLE meal_plan(id INTEGER PRIMARY KEY AUTOINCREMENT, day TEXT NOT NULL, meal TEXT"
            + " NOT NULL, recipe_id TEXT NOT NULL REFERENCES catalog_items(id), portions REAL NOT"
            + " NULL CHECK(portions>0), consumed INTEGER NOT NULL DEFAULT 0 CHECK(consumed IN"
            + " (0,1)))");
    db.execSQL("CREATE INDEX plan_day ON meal_plan(day)");
    db.execSQL("CREATE INDEX plan_recipe ON meal_plan(recipe_id)");
    db.execSQL(
        "CREATE TABLE shopping_checks(day TEXT NOT NULL, item TEXT NOT NULL, checked INTEGER NOT"
            + " NULL DEFAULT 0, PRIMARY KEY(day,item))");
    db.execSQL(
        "CREATE TABLE hydration(day TEXT PRIMARY KEY, ml INTEGER NOT NULL CHECK(ml>=0 AND"
            + " ml<=10000))");
    db.execSQL(
        "CREATE TABLE lesson_progress(id TEXT PRIMARY KEY, completed INTEGER NOT NULL DEFAULT 0,"
            + " video_uri TEXT)");
    db.execSQL("CREATE TABLE app_meta(key TEXT PRIMARY KEY, value TEXT NOT NULL)");
    db.execSQL("INSERT INTO app_meta(key,value) VALUES('catalog_version','3')");
  }

  public void invalidate() {
    cache = null;
    localizer = null;
    byId = Collections.emptyMap();
  }

  public void initialize() throws Exception {
    if (cache == null) {
      localizer = ContentTranslations.load(helper.getReadableDatabase(), language);
      all();
    }
  }

  public List<Recipe> all() throws Exception {
    if (cache != null) return cache;
    SQLiteDatabase db = helper.getReadableDatabase();
    String text = ContentTranslations.expression("t", language);
    Map<String, List<Recipe.Ingredient>> ingredients = new HashMap<>();
    try (Cursor c =
        db.rawQuery(
            "SELECT i.recipe_id,i.product_id,"
                + text
                + ",i.grams FROM recipe_ingredients i JOIN catalog_items p ON p.id=i.product_id"
                + " JOIN catalog_text t ON t.source=p.title ORDER BY i.recipe_id,i.position",
            null)) {
      while (c.moveToNext())
        ingredients
            .computeIfAbsent(c.getString(0), k -> new ArrayList<>())
            .add(
                new Recipe.Ingredient(
                    c.getString(1).substring("product_".length()), c.getString(2), c.getDouble(3)));
    }
    Map<String, List<String>> steps = new HashMap<>();
    try (Cursor c =
        db.rawQuery(
            "SELECT s.recipe_id,"
                + text
                + " FROM recipe_steps s JOIN catalog_text t ON t.source=s.text_key ORDER BY"
                + " s.recipe_id,s.position",
            null)) {
      while (c.moveToNext())
        steps.computeIfAbsent(c.getString(0), k -> new ArrayList<>()).add(c.getString(1));
    }
    List<Recipe> list = new ArrayList<>();
    Map<String, Recipe> index = new HashMap<>();
    String query =
        "SELECT r.id,"
            + text
            + ",r.category,r.product,r.minutes,r.grams,r.kcal,r.protein,r.fat,r.carbs,COALESCE(tags.value,''),COALESCE(a.value,'')"
            + " FROM catalog_items r JOIN catalog_text t ON t.source=r.title LEFT JOIN (SELECT"
            + " item_id,GROUP_CONCAT(tag) value FROM item_tags GROUP BY item_id) tags ON"
            + " tags.item_id=r.id LEFT JOIN (SELECT item_id,GROUP_CONCAT(allergen) value FROM"
            + " item_allergens GROUP BY item_id) a ON a.item_id=r.id ORDER BY r.id";
    try (Cursor c = db.rawQuery(query, null)) {
      while (c.moveToNext()) {
        String id = c.getString(0);
        Recipe recipe =
            new Recipe(
                id,
                c.getString(1),
                c.getString(2),
                c.getInt(3) == 1,
                c.getInt(4),
                c.getDouble(5),
                c.getDouble(6),
                c.getDouble(7),
                c.getDouble(8),
                c.getDouble(9),
                c.getString(10),
                c.getString(11),
                ingredients.getOrDefault(id, Collections.emptyList()),
                steps.getOrDefault(id, Collections.emptyList()));
        list.add(recipe);
        index.put(id, recipe);
      }
    }
    byId = Collections.unmodifiableMap(index);
    cache = Collections.unmodifiableList(list);
    return cache;
  }

  public Recipe recipe(String id) throws Exception {
    all();
    Recipe recipe = byId.get(id);
    if (recipe != null) return recipe;
    throw new IllegalArgumentException("recipe_missing");
  }

  public Set<String> favorites() {
    Set<String> result = new HashSet<>();
    try (Cursor c =
        helper.getReadableDatabase().rawQuery("SELECT recipe_id FROM favorites", null)) {
      while (c.moveToNext()) result.add(c.getString(0));
    }
    return result;
  }

  public void favorite(String id, boolean on) {
    if (on)
      helper
          .getWritableDatabase()
          .execSQL("INSERT OR IGNORE INTO favorites(recipe_id) VALUES(?)", new Object[] {id});
    else helper.getWritableDatabase().delete("favorites", "recipe_id=?", new String[] {id});
  }

  public void plan(String day, String meal, String id, double portions) {
    LocalDate.parse(day);
    validatePortions(portions);
    ContentValues values = new ContentValues();
    values.put("day", day);
    values.put("meal", meal);
    values.put("recipe_id", id);
    values.put("portions", portions);
    helper.getWritableDatabase().insertOrThrow("meal_plan", null, values);
  }

  public static void validatePortions(double portions) {
    if (!Double.isFinite(portions) || portions < 0.25 || portions > 10)
      throw new IllegalArgumentException("portions");
  }

  public List<Planned> plans(String day) throws Exception {
    List<Planned> result = new ArrayList<>();
    try (Cursor c =
        helper
            .getReadableDatabase()
            .rawQuery(
                "SELECT id,meal,recipe_id,portions,consumed FROM meal_plan WHERE day=? ORDER BY id",
                new String[] {day})) {
      while (c.moveToNext())
        result.add(
            new Planned(
                c.getLong(0),
                c.getString(1),
                recipe(c.getString(2)),
                c.getDouble(3),
                c.getInt(4) == 1));
    }
    return result;
  }

  public void deletePlan(long id) {
    helper.getWritableDatabase().delete("meal_plan", "id=?", new String[] {Long.toString(id)});
  }

  public void consume(long id) throws Exception {
    SQLiteDatabase db = helper.getWritableDatabase();
    db.beginTransaction();
    try (Cursor c =
        db.rawQuery(
            "SELECT day,meal,recipe_id,portions,consumed FROM meal_plan WHERE id=?",
            new String[] {Long.toString(id)})) {
      if (!c.moveToFirst() || c.getInt(4) != 0) return;
      if (LocalDate.parse(c.getString(0)).isAfter(LocalDate.now()))
        throw new IllegalArgumentException("future_day");
      Recipe recipe = recipe(c.getString(2));
      double portions = c.getDouble(3);
      ContentValues values = new ContentValues();
      values.put("day", c.getString(0));
      values.put("meal", c.getString(1));
      values.put("name", recipe.title);
      values.put("grams", recipe.grams * portions);
      values.put("kcal", recipe.kcal * portions);
      values.put("protein", recipe.protein * portions);
      values.put("fat", recipe.fat * portions);
      values.put("carbs", recipe.carbs * portions);
      db.insertOrThrow("diary", null, values);
      db.execSQL("UPDATE meal_plan SET consumed=1 WHERE id=?", new Object[] {id});
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }

  public List<Shopping> shopping(String day) throws Exception {
    Map<String, Shopping> result = new TreeMap<>();
    for (Planned plan : plans(day))
      if (!plan.consumed) {
        List<Recipe.Ingredient> ingredients = plan.recipe.ingredients;
        if (plan.recipe.product)
          ingredients =
              Collections.singletonList(
                  new Recipe.Ingredient(
                      plan.recipe.id.substring("product_".length()), plan.recipe.title, 100));
        for (Recipe.Ingredient item : ingredients) {
          if (item.id.equals("water")) continue;
          Shopping existing = result.get(item.id);
          if (existing == null)
            result.put(item.id, new Shopping(item.id, item.name, item.grams * plan.portions));
          else existing.grams += item.grams * plan.portions;
        }
      }
    try (Cursor c =
        helper
            .getReadableDatabase()
            .rawQuery("SELECT item,checked FROM shopping_checks WHERE day=?", new String[] {day})) {
      while (c.moveToNext())
        if (result.containsKey(c.getString(0)))
          result.get(c.getString(0)).checked = c.getInt(1) == 1;
    }
    return new ArrayList<>(result.values());
  }

  public void checkShopping(String day, String item, boolean checked) {
    ContentValues values = new ContentValues();
    values.put("day", day);
    values.put("item", item);
    values.put("checked", checked ? 1 : 0);
    helper
        .getWritableDatabase()
        .insertWithOnConflict("shopping_checks", null, values, SQLiteDatabase.CONFLICT_REPLACE);
  }

  public int water(String day) {
    try (Cursor c =
        helper
            .getReadableDatabase()
            .rawQuery("SELECT ml FROM hydration WHERE day=?", new String[] {day})) {
      return c.moveToFirst() ? c.getInt(0) : 0;
    }
  }

  public void water(String day, int delta) {
    int value = Math.max(0, Math.min(10000, water(day) + delta));
    ContentValues values = new ContentValues();
    values.put("day", day);
    values.put("ml", value);
    helper
        .getWritableDatabase()
        .insertWithOnConflict("hydration", null, values, SQLiteDatabase.CONFLICT_REPLACE);
  }

  public boolean completed(String id) {
    try (Cursor c =
        helper
            .getReadableDatabase()
            .rawQuery("SELECT completed FROM lesson_progress WHERE id=?", new String[] {id})) {
      return c.moveToFirst() && c.getInt(0) == 1;
    }
  }

  public String video(String id) {
    try (Cursor c =
        helper
            .getReadableDatabase()
            .rawQuery("SELECT video_uri FROM lesson_progress WHERE id=?", new String[] {id})) {
      return c.moveToFirst() ? c.getString(0) : null;
    }
  }

  public void lesson(String id, Boolean completed, String video) {
    SQLiteDatabase db = helper.getWritableDatabase();
    db.execSQL("INSERT OR IGNORE INTO lesson_progress(id) VALUES(?)", new Object[] {id});
    ContentValues values = new ContentValues();
    if (completed != null) values.put("completed", completed ? 1 : 0);
    if (video != null) values.put("video_uri", video);
    db.update("lesson_progress", values, "id=?", new String[] {id});
  }

  public static final class Planned {
    public final long id;
    public final String meal;
    public final Recipe recipe;
    public final double portions;
    public final boolean consumed;

    Planned(long id, String meal, Recipe recipe, double portions, boolean consumed) {
      this.id = id;
      this.meal = meal;
      this.recipe = recipe;
      this.portions = portions;
      this.consumed = consumed;
    }
  }

  public static final class Shopping {
    public final String id, name;
    public double grams;
    public boolean checked;

    Shopping(String id, String name, double grams) {
      this.id = id;
      this.name = name;
      this.grams = grams;
    }
  }
}
