package com.nutrifit.app.ui;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.LinearLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nutrifit.app.R;
import com.nutrifit.app.data.LessonCatalog;
import com.nutrifit.app.model.Lesson;
import java.util.*;

final class LearningScreen {
  private final MainActivity a;
  private final View root;
  private final int token;

  LearningScreen(MainActivity a) {
    this.a = a;
    token = a.token();
    root = a.layout(R.layout.screen_learn);
    root.findViewById(R.id.open_plus).setOnClickListener(v -> a.show(R.id.nav_premium));
    a.setBusy(true);
    a.repo.io.execute(
        () -> {
          try {
            List<Lesson> lessons = LessonCatalog.load(a);
            Set<String> completed = new HashSet<>();
            Map<String, String> videos = new HashMap<>();
            for (Lesson lesson : lessons) {
              if (a.repo.library.completed(lesson.id)) completed.add(lesson.id);
              videos.put(lesson.id, a.repo.library.video(lesson.id));
            }
            a.runOnUiThread(
                () -> {
                  if (a.active(token)) {
                    a.setBusy(false);
                    render(lessons, completed, videos);
                  }
                });
          } catch (Exception e) {
            a.runOnUiThread(
                () -> {
                  if (a.active(token)) {
                    a.setBusy(false);
                    Ui.error(a, R.string.storage_error);
                  }
                });
          }
        });
  }

  private void render(List<Lesson> lessons, Set<String> completed, Map<String, String> videos) {
    int total = 0, done = 0;
    for (Lesson lesson : lessons) {
      boolean external = !lesson.url.isEmpty();
      if (!external) {
        total++;
        if (completed.contains(lesson.id)) done++;
      }
      LinearLayout list = root.findViewById(external ? R.id.video_rows : R.id.lesson_rows);
      View row = a.getLayoutInflater().inflate(R.layout.row_lesson, list, false);
      list.addView(row);
      Ui.text(row, R.id.lesson_title, lesson.title);
      Ui.text(row, R.id.lesson_source, a.getString(R.string.link_source, lesson.author));
      Ui.text(
          row,
          R.id.lesson_badge,
          a.getString(
              external
                  ? R.string.video_badge
                  : completed.contains(lesson.id)
                      ? R.string.lesson_completed
                      : lesson.premium ? R.string.plus_tag : R.string.free_tag));
      Ui.text(
          row,
          R.id.lesson_open,
          a.getString(external ? R.string.watch_original : R.string.lesson_open));
      row.findViewById(R.id.lesson_open)
          .setOnClickListener(
              v -> {
                if (external) {
                  try {
                    a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(lesson.url)));
                  } catch (android.content.ActivityNotFoundException e) {
                    Ui.error(a, R.string.no_browser);
                  }
                  return;
                }
                if (lesson.premium && !a.repo.premium()) {
                  a.show(R.id.nav_premium);
                  return;
                }
                open(lesson, completed.contains(lesson.id), videos.get(lesson.id));
              });
    }
    Ui.text(root, R.id.lesson_progress, a.getString(R.string.learn_progress, done, total));
  }

  private void open(Lesson lesson, boolean done, String uri) {
    View view = a.getLayoutInflater().inflate(R.layout.dialog_lesson, null);
    android.widget.ScrollView scroll = new android.widget.ScrollView(a);
    scroll.addView(view);
    androidx.appcompat.app.AlertDialog dialog =
        new MaterialAlertDialogBuilder(a)
            .setTitle(lesson.title)
            .setView(scroll)
            .setNegativeButton(R.string.back, null)
            .create();
    Ui.text(view, R.id.lesson_body, lesson.body);
    Ui.text(
        view,
        R.id.lesson_complete,
        a.getString(done ? R.string.lesson_undo : R.string.lesson_done));
    view.findViewById(R.id.lesson_complete)
        .setOnClickListener(
            v -> {
              dialog.dismiss();
              a.write(
                  () -> a.repo.library.lesson(lesson.id, !done, null),
                  () -> a.show(R.id.nav_learn));
            });
    view.findViewById(R.id.local_play).setVisibility(uri == null ? View.GONE : View.VISIBLE);
    view.findViewById(R.id.local_note).setVisibility(uri == null ? View.VISIBLE : View.GONE);
    view.findViewById(R.id.local_play)
        .setOnClickListener(
            v -> a.startActivity(new Intent(a, VideoActivity.class).putExtra("uri", uri)));
    view.findViewById(R.id.local_attach)
        .setOnClickListener(
            v -> {
              dialog.dismiss();
              a.pendingLesson = lesson.id;
              a.videoPicker.launch(new String[] {"video/*"});
            });
    dialog.show();
  }
}
