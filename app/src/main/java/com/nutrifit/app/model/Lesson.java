package com.nutrifit.app.model;

import org.json.JSONObject;

public final class Lesson {
  public final String id, title, author, body, url;
  public final boolean premium;

  public Lesson(JSONObject o) throws Exception {
    id = o.getString("id");
    title = o.getString("title");
    author = o.getString("author");
    body = o.optString("body");
    url = o.optString("url");
    premium = o.optBoolean("premium");
  }
}
