package com.nutrifit.app.domain;

import java.util.*;
import org.json.*;

/** Immutable text-only translation. IDs, ingredient weights and nutrition never change. */
public final class ContentLocalizer {
  private final Map<String, String> translated;
  private final Map<String, String> originals;

  public ContentLocalizer(String language, JSONObject english, JSONObject polish) throws Exception {
    Map<String, String> target = new HashMap<>(), reverse = new HashMap<>();
    JSONObject selected =
        language.equals("en") ? english : language.equals("pl") ? polish : new JSONObject();
    for (Iterator<String> it = selected.keys(); it.hasNext(); ) {
      String key = it.next();
      target.put(key, selected.getString(key));
    }
    for (JSONObject dictionary : new JSONObject[] {english, polish})
      for (Iterator<String> it = dictionary.keys(); it.hasNext(); ) {
        String key = it.next();
        reverse.putIfAbsent(dictionary.getString(key), key);
      }
    translated = Collections.unmodifiableMap(target);
    originals = Collections.unmodifiableMap(reverse);
  }

  public String text(String original) {
    return translated.getOrDefault(original, original);
  }

  public String snapshot(String stored) {
    String canonical = originals.getOrDefault(stored, stored);
    return text(canonical);
  }

  public JSONObject payload(JSONObject original) throws Exception {
    JSONObject localized = copy(original);
    for (String key : new String[] {"title", "author", "body"})
      if (localized.has(key)) localized.put(key, text(localized.getString(key)));
    JSONArray steps = original.optJSONArray("steps");
    if (steps != null) {
      JSONArray result = new JSONArray();
      for (int i = 0; i < steps.length(); i++) result.put(text(steps.getString(i)));
      localized.put("steps", result);
    }
    JSONArray ingredients = original.optJSONArray("ingredients");
    if (ingredients != null) {
      JSONArray result = new JSONArray();
      for (int i = 0; i < ingredients.length(); i++) {
        JSONObject item = copy(ingredients.getJSONObject(i));
        item.put("name", text(item.getString("name")));
        result.put(item);
      }
      localized.put("ingredients", result);
    }
    return localized;
  }

  private static JSONObject copy(JSONObject source) throws Exception {
    JSONObject result = new JSONObject();
    for (Iterator<String> it = source.keys(); it.hasNext(); ) {
      String key = it.next();
      result.put(key, source.get(key));
    }
    return result;
  }
}
