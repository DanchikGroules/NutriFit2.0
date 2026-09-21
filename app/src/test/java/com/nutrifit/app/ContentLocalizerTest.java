package com.nutrifit.app;

import static org.junit.Assert.*;

import com.nutrifit.app.domain.ContentLocalizer;
import java.util.*;
import org.junit.Test;

public class ContentLocalizerTest {
  @Test
  public void historicalNamesTranslateBothWaysAndCustomTextIsPreserved() {
    Map<String, String> en = Map.of("Овсянка", "Oatmeal"), pl = Map.of("Овсянка", "Owsianka");
    assertEquals("Owsianka", new ContentLocalizer("pl", en, pl).snapshot("Oatmeal"));
    assertEquals("Овсянка", new ContentLocalizer("ru", en, pl).snapshot("Owsianka"));
    assertEquals("My dish", new ContentLocalizer("en", en, pl).snapshot("My dish"));
  }

  @Test
  public void dictionariesAreDefensivelyCopied() {
    Map<String, String> en = new HashMap<>();
    en.put("Овсянка", "Oatmeal");
    ContentLocalizer localizer = new ContentLocalizer("en", en, Collections.emptyMap());
    en.put("Овсянка", "changed");
    assertEquals("Oatmeal", localizer.text("Овсянка"));
  }
}
