package com.nutrifit.app.data;

import android.content.Context;
import com.nutrifit.app.model.Lesson;
import java.io.*;
import java.util.*;
import org.json.JSONArray;

public final class LessonCatalog {
  private static final Map<String, List<Lesson>> cache = new HashMap<>();

  public static synchronized List<Lesson> load(Context context) throws Exception {
    String language = ContentTranslations.language(context);
    if (cache.containsKey(language)) return cache.get(language);
    com.nutrifit.app.domain.ContentLocalizer localizer =
        ContentTranslations.load(context, language);
    try (InputStream stream = context.getAssets().open("lessons.json")) {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      byte[] buffer = new byte[4096];
      int n;
      while ((n = stream.read(buffer)) != -1) bytes.write(buffer, 0, n);
      JSONArray array = new JSONArray(bytes.toString("UTF-8"));
      List<Lesson> result = new ArrayList<>();
      for (int i = 0; i < array.length(); i++)
        result.add(new Lesson(localizer.payload(array.getJSONObject(i))));
      List<Lesson> saved = Collections.unmodifiableList(result);
      cache.put(language, saved);
      return saved;
    }
  }
}
