package com.nutrifit.app.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;

/** Personal/demo caches stay in their respective databases; no text is sent to a server. */
public final class TranslationStore {
  private final SQLiteDatabase db;

  public TranslationStore(SQLiteDatabase db) {
    this.db = db;
  }

  public List<String> pending(String language) {
    validate(language);
    List<String> result = new ArrayList<>();
    try (Cursor c =
        db.rawQuery(
            "SELECT source FROM catalog_text t WHERE source<>'' AND NOT EXISTS(SELECT 1 FROM"
                + " machine_translations m WHERE m.source=t.source AND m.language=?) ORDER BY"
                + " source",
            new String[] {language})) {
      while (c.moveToNext()) result.add(c.getString(0));
    }
    return result;
  }

  public void save(String language, Map<String, String> translations) {
    validate(language);
    db.beginTransaction();
    try {
      for (Map.Entry<String, String> entry : translations.entrySet()) {
        if (entry.getValue() == null || entry.getValue().trim().isEmpty())
          throw new IllegalArgumentException("empty_translation");
        db.execSQL(
            "INSERT OR REPLACE INTO machine_translations(source,language,text) VALUES(?,?,?)",
            new Object[] {entry.getKey(), language, entry.getValue()});
      }
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }

  private static void validate(String language) {
    if (!language.equals("en") && !language.equals("pl"))
      throw new IllegalArgumentException("unsupported_language");
  }
}
