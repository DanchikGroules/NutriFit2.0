package com.nutrifit.app.ui;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.*;
import com.nutrifit.app.data.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Retains download/progress across rotation; cancels work when the translation screen is closed.
 */
public final class TranslationViewModel extends AndroidViewModel {
  public final MutableLiveData<Integer> state = new MutableLiveData<>(0);
  public final MutableLiveData<int[]> progress = new MutableLiveData<>(new int[] {0, 0});
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private Future<?> job;

  public TranslationViewModel(Application app) {
    super(app);
  }

  public void start(String language) {
    if (job != null && !job.isDone()) return;
    state.setValue(1);
    LocalRepository repo = LocalRepository.get(getApplication());
    job =
        worker.submit(
            () -> {
              try {
                TranslationStore store = new TranslationStore(repo.db.getWritableDatabase());
                List<String> pending = store.pending(language);
                if (!pending.isEmpty()) {
                  TranslatorOptions options =
                      new TranslatorOptions.Builder()
                          .setSourceLanguage(TranslateLanguage.RUSSIAN)
                          .setTargetLanguage(language)
                          .build();
                  try (Translator translator = Translation.getClient(options)) {
                    Tasks.await(
                        translator.downloadModelIfNeeded(
                            new DownloadConditions.Builder().requireWifi().build()),
                        5,
                        TimeUnit.MINUTES);
                    state.postValue(2);
                    Map<String, String> results = new LinkedHashMap<>();
                    for (String source : pending) {
                      if (Thread.currentThread().isInterrupted()) return;
                      results.put(
                          source, Tasks.await(translator.translate(source), 60, TimeUnit.SECONDS));
                      progress.postValue(new int[] {results.size(), pending.size()});
                    }
                    if (Thread.currentThread().isInterrupted()) return;
                    store.save(language, results);
                  }
                }
                repo.io.submit(() -> repo.library.invalidate()).get();
                state.postValue(3);
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              } catch (Exception e) {
                state.postValue(4);
              }
            });
  }

  @Override
  protected void onCleared() {
    if (job != null) job.cancel(true);
    worker.shutdownNow();
  }
}
