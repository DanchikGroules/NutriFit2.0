package com.nutrifit.app.data;

import android.content.Context;
import com.nutrifit.app.domain.ContentLocalizer;
import java.io.*;
import java.util.*;
import org.json.JSONObject;

/** Read on the repository IO queue. The three immutable dictionaries are reused per process. */
public final class ContentTranslations {
  private static final Map<String, ContentLocalizer> cache = new HashMap<>();

  public static String language(Context context) {
    return supported(context.getResources().getConfiguration().getLocales().get(0).getLanguage());
  }

  public static String supported(String tag) {
    return tag.equals("en") || tag.equals("pl") ? tag : "ru";
  }

  public static synchronized ContentLocalizer load(Context context, String language)
      throws Exception {
    if (cache.isEmpty()) {
      JSONObject en = read(context, "translations/en.json"),
          pl = read(context, "translations/pl.json");
      for (String tag : new String[] {"ru", "en", "pl"})
        cache.put(tag, new ContentLocalizer(tag, en, pl));
    }
    return cache.get(supported(language));
  }

  private static JSONObject read(Context context, String file) throws Exception {
    try (InputStream stream = context.getAssets().open(file);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[8192];
      int count;
      while ((count = stream.read(buffer)) != -1) bytes.write(buffer, 0, count);
      return new JSONObject(bytes.toString("UTF-8"));
    }
  }
}
