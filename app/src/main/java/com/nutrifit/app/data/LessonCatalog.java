package com.nutrifit.app.data;

import android.content.Context;
import android.database.Cursor;
import com.nutrifit.app.model.Lesson;
import java.util.*;

public final class LessonCatalog {
  public static List<Lesson> load(Context context) {
    String column = ContentTranslations.language(context);
    List<Lesson> result = new ArrayList<>();
    String query =
        "SELECT l.id,"
            + ContentTranslations.expression("t", column)
            + ","
            + ContentTranslations.expression("a", column)
            + ","
            + ContentTranslations.expression("b", column)
            + ",l.url,l.premium FROM lessons l JOIN catalog_text t ON t.source=l.title JOIN"
            + " catalog_text a ON a.source=l.author JOIN catalog_text b ON b.source=l.body ORDER BY"
            + " l.position";
    try (Cursor c = LocalRepository.get(context).db.getReadableDatabase().rawQuery(query, null)) {
      while (c.moveToNext())
        result.add(
            new Lesson(
                c.getString(0),
                c.getString(1),
                c.getString(2),
                c.getString(3),
                c.getString(4),
                c.getInt(5) == 1));
    }
    return Collections.unmodifiableList(result);
  }
}
