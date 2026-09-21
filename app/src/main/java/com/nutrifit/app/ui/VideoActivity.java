package com.nutrifit.app.ui;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.MediaController;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;
import com.nutrifit.app.R;
import com.nutrifit.app.data.LocalRepository;

public class VideoActivity extends AppCompatActivity {
  private VideoView video;
  private int position;
  private boolean playing = true;

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    if (!LocalRepository.get(this).hasAccess()) {
      finish();
      return;
    }
    setContentView(R.layout.activity_video);
    Ui.insets(findViewById(R.id.root));
    video = findViewById(R.id.video);
    findViewById(R.id.video_back).setOnClickListener(v -> finish());
    if (state != null) {
      position = state.getInt("position");
      playing = state.getBoolean("playing", false);
    }
    MediaController controls = new MediaController(this);
    controls.setAnchorView(video);
    video.setMediaController(controls);
    video.setOnErrorListener(
        (player, what, extra) -> {
          findViewById(R.id.video_error).setVisibility(View.VISIBLE);
          return true;
        });
    video.setOnPreparedListener(
        player -> {
          video.seekTo(position);
          if (playing) video.start();
        });
  }

  @Override
  protected void onStart() {
    super.onStart();
    if (video != null)
      try {
        Uri uri = Uri.parse(getIntent().getStringExtra("uri"));
        if (!"content".equals(uri.getScheme())) throw new IllegalArgumentException();
        video.setVideoURI(uri);
      } catch (Exception e) {
        findViewById(R.id.video_error).setVisibility(View.VISIBLE);
      }
  }

  @Override
  protected void onPause() {
    if (video != null) {
      position = video.getCurrentPosition();
      playing = video.isPlaying();
      video.pause();
    }
    super.onPause();
  }

  @Override
  protected void onStop() {
    if (video != null) video.stopPlayback();
    super.onStop();
  }

  @Override
  protected void onSaveInstanceState(Bundle out) {
    out.putInt("position", video == null ? position : video.getCurrentPosition());
    out.putBoolean("playing", video != null && video.isPlaying());
    super.onSaveInstanceState(out);
  }
}
