package com.nutrifit.app;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.platform.app.InstrumentationRegistry;
import com.nutrifit.app.data.*;
import java.io.File;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.*;

public class LocalStorageTest {
  @Test
  public void versionTwoUpgradePreservesAccountPlansAndProgress() throws Exception {
    LocalAccount account = new LocalAccount(context);
    account.register("Owner", "owner@example.com", "password123".toCharArray());
    SQLiteDatabase old = context.openOrCreateDatabase("nutrifit.db", 0, null);
    old.execSQL(
        "CREATE TABLE diary(id INTEGER PRIMARY KEY,day TEXT,meal TEXT,name TEXT,grams REAL,kcal"
            + " REAL,protein REAL,fat REAL,carbs REAL)");
    old.execSQL("CREATE TABLE weights(day TEXT PRIMARY KEY,kg REAL)");
    old.execSQL(
        "CREATE TABLE recipes(id TEXT PRIMARY KEY,payload TEXT NOT NULL,product INTEGER NOT NULL)");
    old.execSQL(
        "CREATE TABLE favorites(recipe_id TEXT PRIMARY KEY REFERENCES recipes(id) ON DELETE"
            + " CASCADE)");
    old.execSQL(
        "CREATE TABLE meal_plan(id INTEGER PRIMARY KEY AUTOINCREMENT,day TEXT,meal TEXT,recipe_id"
            + " TEXT REFERENCES recipes(id),portions REAL,consumed INTEGER)");
    old.execSQL("CREATE INDEX plan_day ON meal_plan(day)");
    old.execSQL(
        "CREATE TABLE shopping_checks(day TEXT,item TEXT,checked INTEGER,PRIMARY KEY(day,item))");
    old.execSQL("CREATE TABLE hydration(day TEXT PRIMARY KEY,ml INTEGER)");
    old.execSQL(
        "CREATE TABLE lesson_progress(id TEXT PRIMARY KEY,completed INTEGER,video_uri TEXT)");
    old.execSQL("CREATE TABLE app_meta(key TEXT PRIMARY KEY,value TEXT)");
    old.execSQL("INSERT INTO recipes VALUES('recipe_001','{}',0)");
    old.execSQL("INSERT INTO favorites VALUES('recipe_001')");
    old.execSQL("INSERT INTO meal_plan VALUES(42,'2026-01-01','breakfast','recipe_001',2,1)");
    old.execSQL(
        "INSERT INTO diary VALUES(1,'2026-01-01','breakfast','My breakfast',100,200,1,2,3)");
    old.execSQL("INSERT INTO weights VALUES('2026-01-01',72)");
    old.execSQL("INSERT INTO hydration VALUES('2026-01-01',500)");
    old.execSQL("INSERT INTO shopping_checks VALUES('2026-01-01','oats',1)");
    old.execSQL("INSERT INTO lesson_progress VALUES('basics',1,'content://saved/video')");
    old.execSQL("INSERT INTO app_meta VALUES('catalog_version','1')");
    old.setVersion(2);
    old.close();
    library.initialize("en");
    assertEquals(3, db.getReadableDatabase().getVersion());
    assertEquals(220, library.all().size());
    assertTrue(library.favorites().contains("recipe_001"));
    assertEquals(42, library.plans("2026-01-01").get(0).id);
    assertTrue(library.plans("2026-01-01").get(0).consumed);
    assertEquals(2, library.plans("2026-01-01").get(0).portions, 0);
    assertEquals("My breakfast", db.diary("2026-01-01").get(0).name);
    assertEquals(72, db.weights().get(0).kg, 0);
    assertEquals(500, library.water("2026-01-01"));
    assertTrue(library.completed("basics"));
    assertEquals("content://saved/video", library.video("basics"));
    try (android.database.Cursor c =
        db.getReadableDatabase().rawQuery("PRAGMA foreign_key_check", null)) {
      assertEquals(0, c.getCount());
    }
    try (android.database.Cursor c =
        db.getReadableDatabase()
            .rawQuery("SELECT name FROM sqlite_master WHERE name='recipes'", null)) {
      assertEquals(0, c.getCount());
    }
    try (android.database.Cursor c =
        db.getReadableDatabase()
            .rawQuery("SELECT checked FROM shopping_checks WHERE item='oats'", null)) {
      assertTrue(c.moveToFirst());
      assertEquals(1, c.getInt(0));
    }
    account.logout();
    assertTrue(account.login("owner@example.com", "password123".toCharArray()));
    db.close();
    db = new AppDatabase(context);
    library = new LibraryStore(context, db);
    library.initialize("pl");
    assertEquals(42, library.plans("2026-01-01").get(0).id);
  }

  @Test
  public void machineTranslationsAreCachedAndFailedBatchRollsBack() throws Exception {
    library.initialize("ru");
    String source = library.recipe("recipe_001").title;
    TranslationStore store = new TranslationStore(db.getWritableDatabase());
    int before = store.pending("en").size();
    store.save("en", java.util.Collections.singletonMap(source, "Machine oatmeal"));
    assertEquals(before - 1, store.pending("en").size());
    library.invalidate();
    library.initialize("en");
    assertEquals("Machine oatmeal", library.recipe("recipe_001").title);
    assertEquals("Machine oatmeal", library.displayName("Oatmeal with banana"));
    java.util.Map<String, String> invalid = new java.util.LinkedHashMap<>();
    invalid.put(source, "Changed");
    invalid.put("missing-source", "invalid");
    try {
      store.save("en", invalid);
      fail("Foreign key should reject missing source");
    } catch (android.database.sqlite.SQLiteConstraintException expected) {
    }
    library.invalidate();
    library.initialize("en");
    assertEquals("Machine oatmeal", library.recipe("recipe_001").title);
    library.initialize("pl");
    assertEquals("Owsianka z bananem", library.recipe("recipe_001").title);
    assertEquals("Owsianka z bananem", library.displayName("Machine oatmeal"));
  }

  @Test
  public void allCatalogContentLoadsInThreeLanguages() throws Exception {
    for (String language : new String[] {"ru", "en", "pl"}) {
      library.initialize(language);
      assertEquals(220, library.all().size());
      int recipes = 0;
      for (com.nutrifit.app.model.Recipe recipe : library.all()) {
        assertTrue(recipe.grams > 0);
        if (!recipe.product) {
          recipes++;
          assertTrue(recipe.ingredients.size() >= 3);
          assertTrue(recipe.steps.size() >= 3);
        }
        if (!language.equals("ru")) {
          assertFalse(recipe.title.matches("(?s).*[А-Яа-яЁё].*"));
          for (String step : recipe.steps) assertFalse(step.matches("(?s).*[А-Яа-яЁё].*"));
          for (com.nutrifit.app.model.Recipe.Ingredient i : recipe.ingredients)
            assertFalse(i.name.matches("(?s).*[А-Яа-яЁё].*"));
        }
      }
      assertEquals(120, recipes);
    }
  }

  @Test
  public void demoDatabaseIsSeparate() throws Exception {
    library.initialize();
    library.water("2026-01-01", 750);
    DemoContext guest = new DemoContext(context);
    AppDatabase guestDb = new AppDatabase(guest);
    try {
      LibraryStore guestLibrary = new LibraryStore(guest, guestDb);
      guestLibrary.initialize();
      assertEquals(0, guestLibrary.water("2026-01-01"));
      guestLibrary.water("2026-01-01", 250);
      assertEquals(750, library.water("2026-01-01"));
    } finally {
      guestDb.close();
      guest.deleteDatabase("nutrifit.db");
    }
  }

  @Test
  public void switchingLanguagesPreservesDatabaseAndRefreshesCache() throws Exception {
    library.initialize("ru");
    String day = LocalDate.now().toString();
    String id = "recipe_001";
    library.favorite(id, true);
    library.plan(day, "breakfast", id, 1);
    String russian = library.recipe(id).title;
    library.initialize("en");
    assertEquals("Oatmeal with banana", library.recipe(id).title);
    assertEquals("Rolled oats, dry", library.recipe(id).ingredients.get(0).name);
    library.consume(library.plans(day).get(0).id);
    assertEquals(1, db.diary(day).size());
    library.initialize("pl");
    assertEquals("Owsianka z bananem", library.recipe(id).title);
    assertEquals("Owsianka z bananem", library.displayName(db.diary(day).get(0).name));
    assertTrue(library.favorites().contains(id));
    assertTrue(library.plans(day).get(0).consumed);
    library.initialize("ru");
    assertEquals(russian, library.recipe(id).title);
    assertEquals(220, library.all().size());
    try (android.database.Cursor c =
        db.getReadableDatabase()
            .rawQuery("SELECT title FROM catalog_items WHERE id=?", new String[] {id})) {
      assertTrue(c.moveToFirst());
      assertEquals(russian, c.getString(0));
    }
  }

  private Context context;
  private AppDatabase db;
  private LibraryStore library;

  @Before
  public void setup() {
    Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
    String prefix = "test_" + UUID.randomUUID() + "_";
    context =
        new ContextWrapper(base) {
          @Override
          public File getDatabasePath(String name) {
            return super.getDatabasePath(prefix + name);
          }

          @Override
          public SQLiteDatabase openOrCreateDatabase(
              String n, int m, SQLiteDatabase.CursorFactory f) {
            return super.openOrCreateDatabase(prefix + n, m, f);
          }

          @Override
          public SQLiteDatabase openOrCreateDatabase(
              String n, int m, SQLiteDatabase.CursorFactory f, DatabaseErrorHandler h) {
            return super.openOrCreateDatabase(prefix + n, m, f, h);
          }

          @Override
          public boolean deleteDatabase(String n) {
            return super.deleteDatabase(prefix + n);
          }

          @Override
          public SharedPreferences getSharedPreferences(String n, int m) {
            return super.getSharedPreferences(prefix + n, m);
          }

          @Override
          public boolean deleteSharedPreferences(String n) {
            return super.deleteSharedPreferences(prefix + n);
          }
        };
    db = new AppDatabase(context);
    library = new LibraryStore(context, db);
  }

  @After
  public void cleanup() {
    db.close();
    context.deleteDatabase("nutrifit.db");
    context.deleteSharedPreferences("local_account");
  }

  @Test
  public void migrationPreservesDiaryAndSeedsCatalog() throws Exception {
    SQLiteDatabase old = context.openOrCreateDatabase("nutrifit.db", 0, null);
    old.execSQL(
        "CREATE TABLE diary(id INTEGER PRIMARY KEY,day TEXT,meal TEXT,name TEXT,grams REAL,kcal"
            + " REAL,protein REAL,fat REAL,carbs REAL)");
    old.execSQL("CREATE TABLE weights(day TEXT PRIMARY KEY,kg REAL)");
    old.execSQL("INSERT INTO diary VALUES(1,'2026-01-01','meal','saved',100,200,1,2,3)");
    old.execSQL("INSERT INTO weights VALUES('2026-01-01',70)");
    old.setVersion(1);
    old.close();
    library.initialize();
    assertEquals(220, library.all().size());
    assertEquals("saved", db.diary("2026-01-01").get(0).name);
    assertEquals(70, db.weights().get(0).kg, 0);
    library.initialize();
    assertEquals(220, library.all().size());
  }

  @Test
  public void planConsumptionIsIdempotentAndShoppingAggregates() throws Exception {
    library.initialize();
    String day = LocalDate.now().toString();
    String id = "recipe_001";
    library.plan(day, "breakfast", id, 1);
    library.plan(day, "lunch", id, 2);
    double grams = library.recipe(id).ingredients.get(0).grams;
    String ingredient = library.recipe(id).ingredients.get(0).id;
    assertEquals(
        grams * 3,
        library.shopping(day).stream().filter(x -> x.id.equals(ingredient)).findFirst().get().grams,
        .001);
    long plan = library.plans(day).get(0).id;
    library.consume(plan);
    library.consume(plan);
    assertEquals(1, db.diary(day).size());
    assertTrue(library.plans(day).get(0).consumed);
    assertEquals(
        grams * 2,
        library.shopping(day).stream().filter(x -> x.id.equals(ingredient)).findFirst().get().grams,
        .001);
  }

  @Test
  public void localSelectionsPersistAfterReopen() throws Exception {
    library.initialize();
    library.favorite("recipe_001", true);
    library.water("2026-01-01", 250);
    library.lesson("basics", true, "content://demo/video");
    db.close();
    db = new AppDatabase(context);
    library = new LibraryStore(context, db);
    library.initialize();
    assertTrue(library.favorites().contains("recipe_001"));
    assertEquals(250, library.water("2026-01-01"));
    assertTrue(library.completed("basics"));
    assertEquals("content://demo/video", library.video("basics"));
    library.water("2026-01-01", 20000);
    assertEquals(10000, library.water("2026-01-01"));
    library.water("2026-01-01", -20000);
    assertEquals(0, library.water("2026-01-01"));
  }

  @Test
  public void passwordChangeAndRecoveryKeepAccount() throws Exception {
    LocalAccount account = new LocalAccount(context);
    String code = account.register("Student", "test@example.com", "password123".toCharArray());
    assertTrue(account.signedIn());
    assertTrue(account.changePassword("password123".toCharArray(), "changed123".toCharArray()));
    account.logout();
    assertFalse(account.login("test@example.com", "password123".toCharArray()));
    assertTrue(account.login("test@example.com", "changed123".toCharArray()));
    assertTrue(account.reset("test@example.com", code, "recovered123".toCharArray()));
    assertFalse(account.signedIn());
    assertTrue(account.login("test@example.com", "recovered123".toCharArray()));
    assertEquals("Student", account.name());
  }
}
