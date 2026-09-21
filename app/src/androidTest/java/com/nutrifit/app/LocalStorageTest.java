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
            .rawQuery("SELECT payload FROM recipes WHERE id=?", new String[] {id})) {
      assertTrue(c.moveToFirst());
      assertEquals(russian, new org.json.JSONObject(c.getString(0)).getString("title"));
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
