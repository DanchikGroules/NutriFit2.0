package com.nutrifit.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.SparseArray;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.nutrifit.app.R;
import com.nutrifit.app.data.LocalRepository;
import com.nutrifit.app.model.Profile;
import java.time.LocalDate;

/** Navigation and lifecycle only; each screen binds its own XML layout. */
public class MainActivity extends AppCompatActivity {
  LocalRepository repo;
  Profile profile;
  LocalDate day = LocalDate.now();
  private int current = R.id.nav_diary, generation;
  private boolean busy;
  private FrameLayout content;
  private BottomNavigationView navigation;
  private SparseArray<Parcelable> restoredViews;
  private Profile restoredDraft;
  private ProfileScreen profileScreen;
  String pendingLesson;
  final androidx.activity.result.ActivityResultLauncher<String[]> videoPicker =
      registerForActivityResult(
          new androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
          uri -> {
            if (uri == null || pendingLesson == null) return;
            String lesson = pendingLesson;
            pendingLesson = null;
            try {
              getContentResolver()
                  .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
              write(
                  () -> repo.library.lesson(lesson, null, uri.toString()),
                  () -> {
                    saved();
                    show(R.id.nav_learn);
                  });
            } catch (Exception e) {
              Ui.error(this, R.string.video_error);
            }
          });

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    repo = LocalRepository.get(this);
    if (!repo.hasAccess()) {
      openAuth();
      return;
    }
    setContentView(R.layout.activity_main);
    Ui.insets(findViewById(R.id.root));
    content = findViewById(R.id.content);
    navigation = findViewById(R.id.navigation);
    if(repo.isDemo)((com.google.android.material.appbar.MaterialToolbar)findViewById(R.id.toolbar)).setSubtitle(R.string.demo_badge);
    if (state != null) {
      day = LocalDate.parse(state.getString("day", day.toString()));
      current = state.getInt("screen", current);
      restoredViews = state.getSparseParcelableArray("fields");
      restoredDraft = (Profile) state.getSerializable("draft");
      pendingLesson = state.getString("pendingLesson");
    }
    navigation.setOnItemSelectedListener(
        item -> {
          if (busy) return false;
          if (current != item.getItemId()) {
            restoredViews = null;
            restoredDraft = null;
            show(item.getItemId());
          }
          return true;
        });
    load();
    getOnBackPressedDispatcher()
        .addCallback(
            this,
            new androidx.activity.OnBackPressedCallback(true) {
              @Override
              public void handleOnBackPressed() {
                if (busy) return;
                if (current != R.id.nav_diary && profile != null) show(R.id.nav_diary);
                else {
                  setEnabled(false);
                  getOnBackPressedDispatcher().onBackPressed();
                }
              }
            });
  }

  private void load() {
    setBusy(true);
    String language = com.nutrifit.app.data.ContentTranslations.language(this);
    repo.io.execute(
        () -> {
          try {
            repo.library.initialize(language);
            Profile loaded = repo.profiles.load();
            runOnUiThread(
                () -> {
                  if (isDestroyed() || isFinishing()) return;
                  if (!repo.hasAccess()) {
                    openAuth();
                    return;
                  }
                  profile = loaded;
                  setBusy(false);
                  show(profile == null ? R.id.nav_profile : current);
                  StartupMotion.play(content);
                  if (restoredViews != null) {
                    content.restoreHierarchyState(restoredViews);
                    restoredViews = null;
                  }
                });
          } catch (Exception e) {
            runOnUiThread(
                () -> {
                  if (!isDestroyed() && !isFinishing()) {
                    setBusy(false);
                    Ui.error(this, R.string.storage_error);
                  }
                });
          }
        });
  }

  void show(int screen) {
    current = screen;
    generation++;
    profileScreen = null;
    navigation.setVisibility(profile == null ? View.GONE : View.VISIBLE);
    if (navigation.getMenu().findItem(screen) != null && navigation.getSelectedItemId() != screen)
      navigation.setSelectedItemId(screen);
    content.removeAllViews();
    if (screen == R.id.nav_profile) {
      profileScreen = new ProfileScreen(this, restoredDraft);
      restoredDraft = null;
    } else if (screen == R.id.nav_progress) new WeightScreen(this);
    else if (screen == R.id.nav_library) new LibraryScreen(this);
    else if (screen == R.id.nav_plan) {
      if (repo.premium()) new PlanScreen(this);
      else new PremiumScreen(this);
    } else if (screen == R.id.nav_learn) new LearningScreen(this);
    else if (screen == R.id.nav_premium) new PremiumScreen(this);
    else new DiaryScreen(this);
  }

  View layout(int resource) {
    View v = getLayoutInflater().inflate(resource, content, false);
    if (resource == R.layout.screen_library)
      content.addView(v, new FrameLayout.LayoutParams(-1, -1));
    else {
      android.widget.ScrollView scroll = new android.widget.ScrollView(this);
      scroll.setFillViewport(true);
      scroll.addView(v);
      content.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
    }
    return v;
  }

  int token() {
    return generation;
  }

  boolean active(int token) {
    return !isDestroyed() && !isFinishing() && generation == token;
  }

  void setBusy(boolean value) {
    busy = value;
    findViewById(R.id.busy).setVisibility(value ? View.VISIBLE : View.GONE);
    Ui.enabled(content, !value);
    Ui.enabled(navigation, !value);
  }

  void write(Runnable work, Runnable after) {
    if (busy) return;
    setBusy(true);
    int token = token();
    repo.io.execute(
        () -> {
          try {
            work.run();
            Profile saved = repo.profiles.load();
            runOnUiThread(
                () -> {
                  if (active(token)) {
                    profile = saved;
                    setBusy(false);
                    after.run();
                  }
                });
          } catch (Exception e) {
            runOnUiThread(
                () -> {
                  if (active(token)) {
                    setBusy(false);
                    Ui.error(this, R.string.storage_error);
                  }
                });
          }
        });
  }

  void saved() {
    Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
  }

  void openAuth() {
    startActivity(
        new Intent(this, AuthActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    finish();
  }

  @Override
  protected void onSaveInstanceState(Bundle out) {
    if (content != null) {
      SparseArray<Parcelable> fields = new SparseArray<>();
      content.saveHierarchyState(fields);
      out.putSparseParcelableArray("fields", fields);
      out.putInt("screen", current);
      out.putString("day", day.toString());
      out.putString("pendingLesson", pendingLesson);
      if (profileScreen != null) out.putSerializable("draft", profileScreen.draft);
    }
    super.onSaveInstanceState(out);
  }

  void openRecipe(String id) {
    startActivity(
        new Intent(this, RecipeActivity.class)
            .putExtra("recipe", id)
            .putExtra("day", day.toString()));
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (profile != null && !busy && current == R.id.nav_diary) show(current);
  }
}
