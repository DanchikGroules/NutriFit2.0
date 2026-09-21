package com.nutrifit.app.data;

import android.content.Context;
import com.nutrifit.app.domain.ContentLocalizer;
import java.util.*;

/** Read on the repository IO queue. The three immutable dictionaries are reused per process. */
public final class ContentTranslations {

  public static String language(Context context) {
    return supported(context.getResources().getConfiguration().getLocales().get(0).getLanguage());
  }

  public static String supported(String tag) {
    return tag.equals("en") || tag.equals("pl") ? tag : "ru";
  }

  public static ContentLocalizer load(android.database.sqlite.SQLiteDatabase db, String language) {
    Map<String, String> en = new HashMap<>(), pl = new HashMap<>();
    try (android.database.Cursor c =
        db.rawQuery(
            "SELECT t.source,"
                + expression("t", "en")
                + ","
                + expression("t", "pl")
                + " FROM catalog_text t",
            null)) {
      while (c.moveToNext()) {
        en.put(c.getString(0), c.getString(1));
        pl.put(c.getString(0), c.getString(2));
      }
    }
    Map<String, String> historic = new HashMap<>();
    try (android.database.Cursor c = db.rawQuery("SELECT source,en,pl FROM catalog_text", null)) {
      while (c.moveToNext()) {
        historic.putIfAbsent(c.getString(1), c.getString(0));
        historic.putIfAbsent(c.getString(2), c.getString(0));
      }
    }
    return new ContentLocalizer(supported(language), en, pl, historic);
  }

  static String expression(String alias, String language) {
    String column = column(language);
    return column.equals("source")
        ? alias + ".source"
        : "COALESCE((SELECT m.text FROM machine_translations m WHERE m.source="
            + alias
            + ".source AND m.language='"
            + column
            + "'),"
            + alias
            + "."
            + column
            + ")";
  }

  static String column(String language) {
    return supported(language).equals("ru") ? "source" : supported(language);
  }
}
