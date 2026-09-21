package com.nutrifit.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.nutrifit.app.model.Recipe;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

/** All methods run on LocalRepository.io. Catalog is seeded transactionally and queried locally. */
public final class LibraryStore {
  private final AppDatabase helper;
  private final Context context;
  private List<Recipe> cache;
  private List<JSONObject> payloads;
  private List<Boolean> productFlags;
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
        "CREATE TABLE recipes(id TEXT PRIMARY KEY, payload TEXT NOT NULL, product INTEGER NOT NULL"
            + " CHECK(product IN (0,1)))");
    db.execSQL(
        "CREATE TABLE favorites(recipe_id TEXT PRIMARY KEY REFERENCES recipes(id) ON DELETE"
            + " CASCADE)");
    db.execSQL(
        "CREATE TABLE meal_plan(id INTEGER PRIMARY KEY AUTOINCREMENT, day TEXT NOT NULL, meal TEXT"
            + " NOT NULL, recipe_id TEXT NOT NULL REFERENCES recipes(id), portions REAL NOT NULL"
            + " CHECK(portions>0), consumed INTEGER NOT NULL DEFAULT 0 CHECK(consumed IN (0,1)))");
    db.execSQL("CREATE INDEX plan_day ON meal_plan(day)");
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
  }

  public void initialize() throws Exception {
    localizer = ContentTranslations.load(context, language);
    SQLiteDatabase db = helper.getWritableDatabase();
    try (Cursor c = db.rawQuery("SELECT value FROM app_meta WHERE key='catalog_version'", null)) {
      if (!c.moveToFirst()) {
        db.beginTransaction();
        try {
          for (String file : new String[] {"recipes.json", "products.json"}) {
            boolean product = file.equals("products.json");
            JSONArray data;
            try (InputStream stream = context.getAssets().open(file)) {
              java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
              byte[] buffer = new byte[8192];
              int n;
              while ((n = stream.read(buffer)) != -1) out.write(buffer, 0, n);
              data = new JSONArray(out.toString(StandardCharsets.UTF_8.name()));
            }
            for (int i = 0; i < data.length(); i++) {
              JSONObject object = data.getJSONObject(i);
              ContentValues values = new ContentValues();
              values.put("id", (product ? "product_" : "") + object.getString("id"));
              values.put("payload", object.toString());
              values.put("product", product ? 1 : 0);
              db.insertOrThrow("recipes", null, values);
            }
          }
          db.execSQL("INSERT INTO app_meta(key,value) VALUES('catalog_version','1')");
          db.setTransactionSuccessful();
        } finally {
          db.endTransaction();
        }
      }
    }
    all();
  }

  public List<Recipe> all() throws Exception {
    if (cache == null) {
      List<Recipe> list = new ArrayList<>();
      if (payloads == null) {
        payloads = new ArrayList<>();
        productFlags = new ArrayList<>();
        try (Cursor c =
            helper
                .getReadableDatabase()
                .rawQuery("SELECT payload,product FROM recipes ORDER BY id", null)) {
          while (c.moveToNext()) {
            payloads.add(new JSONObject(c.getString(0)));
            productFlags.add(c.getInt(1) == 1);
          }
        }
      }
      if (localizer == null) localizer = ContentTranslations.load(context, language);
      Map<String, Recipe> index = new HashMap<>();
      for (int i = 0; i < payloads.size(); i++) {
        Recipe recipe = new Recipe(localizer.payload(payloads.get(i)), productFlags.get(i));
        list.add(recipe);
        index.put(recipe.id, recipe);
      }
      byId = Collections.unmodifiableMap(index);
      cache = Collections.unmodifiableList(list);
    }
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
