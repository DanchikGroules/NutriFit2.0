package com.nutrifit.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;

/** Imports the bundled relational catalog inside SQLiteOpenHelper's create/upgrade transaction. */
final class CatalogDatabase {
  private static final String[] TABLES = {
    "catalog_text",
    "catalog_items",
    "item_tags",
    "item_allergens",
    "recipe_ingredients",
    "recipe_steps",
    "lessons"
  };

  static void install(Context context, SQLiteDatabase target) {
    File temporary = null;
    try {
      temporary = File.createTempFile("catalog-", ".db", context.getCacheDir());
      try (InputStream in = context.getAssets().open("catalog.db");
          OutputStream out = new FileOutputStream(temporary)) {
        byte[] buffer = new byte[16384];
        int count;
        while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
      }
      try (SQLiteDatabase source =
          SQLiteDatabase.openDatabase(temporary.getPath(), null, SQLiteDatabase.OPEN_READONLY)) {
        for (String table : TABLES) {
          try (Cursor schema =
              source.rawQuery(
                  "SELECT sql FROM sqlite_master WHERE type='table' AND name=?",
                  new String[] {table})) {
            if (!schema.moveToFirst()) throw new IllegalStateException("catalog_schema_missing");
            target.execSQL(schema.getString(0));
          }
          try (Cursor rows = source.rawQuery("SELECT * FROM " + table, null)) {
            ContentValues values = new ContentValues();
            while (rows.moveToNext()) {
              values.clear();
              DatabaseUtils.cursorRowToContentValues(rows, values);
              target.insertOrThrow(table, null, values);
            }
          }
        }
        try (Cursor indexes =
            source.rawQuery(
                "SELECT sql FROM sqlite_master WHERE type='index' AND sql IS NOT NULL", null)) {
          while (indexes.moveToNext()) target.execSQL(indexes.getString(0));
        }
        target.execSQL(
            "CREATE TABLE machine_translations(source TEXT NOT NULL REFERENCES catalog_text(source)"
                + " ON DELETE CASCADE, language TEXT NOT NULL CHECK(language IN ('en','pl')), text"
                + " TEXT NOT NULL, PRIMARY KEY(source,language))");
      }
    } catch (IOException e) {
      throw new IllegalStateException("catalog_import_failed", e);
    } finally {
      if (temporary != null && !temporary.delete()) temporary.deleteOnExit();
    }
  }

  /** Stable IDs preserve favorites and plans. Any invalid reference aborts the entire upgrade. */
  static void migrateVersionTwo(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE favorites_new(recipe_id TEXT PRIMARY KEY REFERENCES catalog_items(id) ON"
            + " DELETE CASCADE)");
    db.execSQL("INSERT INTO favorites_new SELECT recipe_id FROM favorites");
    db.execSQL(
        "CREATE TABLE meal_plan_new(id INTEGER PRIMARY KEY AUTOINCREMENT, day TEXT NOT NULL, meal"
            + " TEXT NOT NULL, recipe_id TEXT NOT NULL REFERENCES catalog_items(id), portions REAL"
            + " NOT NULL CHECK(portions>0), consumed INTEGER NOT NULL DEFAULT 0 CHECK(consumed IN"
            + " (0,1)))");
    db.execSQL(
        "INSERT INTO meal_plan_new SELECT id,day,meal,recipe_id,portions,consumed FROM meal_plan");
    db.execSQL("DROP TABLE favorites");
    db.execSQL("DROP TABLE meal_plan");
    db.execSQL("ALTER TABLE favorites_new RENAME TO favorites");
    db.execSQL("ALTER TABLE meal_plan_new RENAME TO meal_plan");
    db.execSQL("CREATE INDEX plan_day ON meal_plan(day)");
    db.execSQL("CREATE INDEX plan_recipe ON meal_plan(recipe_id)");
    db.execSQL("DROP TABLE recipes");
    db.execSQL("INSERT OR REPLACE INTO app_meta(key,value) VALUES('catalog_version','3')");
  }
}
