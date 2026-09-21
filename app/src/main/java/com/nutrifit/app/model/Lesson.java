package com.nutrifit.app.model;

public final class Lesson {
  public final String id, title, author, body, url;
  public final boolean premium;

  public Lesson(String id, String title, String author, String body, String url, boolean premium) {
    this.id = id;
    this.title = title;
    this.author = author;
    this.body = body;
    this.url = url;
    this.premium = premium;
  }
}
