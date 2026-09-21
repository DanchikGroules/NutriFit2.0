package com.nutrifit.app.domain;

import java.util.*;

/** Immutable dictionaries for translating historic diary names without rewriting user entries. */
public final class ContentLocalizer {
  private final Map<String, String> translated, originals;

  public ContentLocalizer(
      String language, Map<String, String> english, Map<String, String> polish) {
    this(language, english, polish, Collections.emptyMap());
  }

  public ContentLocalizer(
      String language,
      Map<String, String> english,
      Map<String, String> polish,
      Map<String, String> historic) {
    translated =
        Collections.unmodifiableMap(
            new HashMap<>(
                language.equals("en")
                    ? english
                    : language.equals("pl") ? polish : Collections.emptyMap()));
    Map<String, String> reverse = new HashMap<>(historic);
    for (Map<String, String> dictionary : Arrays.asList(english, polish))
      for (Map.Entry<String, String> entry : dictionary.entrySet())
        reverse.putIfAbsent(entry.getValue(), entry.getKey());
    originals = Collections.unmodifiableMap(reverse);
  }

  public String text(String original) {
    return translated.getOrDefault(original, original);
  }

  public String snapshot(String stored) {
    return text(originals.getOrDefault(stored, stored));
  }
}
