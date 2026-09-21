package com.nutrifit.app.ui;

import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.content.Context;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import com.nutrifit.app.R;

/** One short entrance per process, with no artificial splash delay or blocked controls. */
final class StartupMotion {
  private static boolean played;

  static void play(View view) {
    if (played) return;
    played = true;
    AccessibilityManager accessibility = (AccessibilityManager) view.getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
    if (!ValueAnimator.areAnimatorsEnabled() || (accessibility != null && accessibility.isTouchExplorationEnabled())) return;
    view.post(() -> {
      if (!view.isAttachedToWindow()) return;
      ViewGroup host = view.getRootView().findViewById(android.R.id.content);
      if (host == null) return;
      View overlay = LayoutInflater.from(view.getContext()).inflate(R.layout.overlay_startup, host, false);
      host.addView(overlay);
      Runnable dismiss = () -> { if (overlay.getParent() == host) host.removeView(overlay); };
      overlay.setOnClickListener(v -> { overlay.animate().cancel(); dismiss.run(); });
      View halo = overlay.findViewById(R.id.startup_halo);
      halo.setScaleX(.65f); halo.setScaleY(.65f); halo.setAlpha(0f);
      halo.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(550).setInterpolator(new OvershootInterpolator(.7f)).start();
      View leaf = overlay.findViewById(R.id.startup_leaf);
      leaf.setScaleX(.55f); leaf.setScaleY(.55f); leaf.setRotation(-25f); leaf.setAlpha(0f);
      leaf.animate().scaleX(1f).scaleY(1f).rotation(0f).alpha(1f).setDuration(580).setInterpolator(new OvershootInterpolator(.8f)).start();
      reveal(overlay.findViewById(R.id.startup_wordmark), 160);
      reveal(overlay.findViewById(R.id.startup_caption), 260);
      overlay.animate().alpha(0f).setStartDelay(760).setDuration(220).withEndAction(dismiss).start();
    });
  }

  private static void reveal(View view, long delay) {
    view.setAlpha(0f);
    view.setTranslationY(12f * view.getResources().getDisplayMetrics().density);
    view.animate().alpha(1f).translationY(0f).setStartDelay(delay).setDuration(360).setInterpolator(new DecelerateInterpolator()).start();
  }
}
