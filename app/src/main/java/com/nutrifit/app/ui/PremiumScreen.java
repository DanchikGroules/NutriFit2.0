package com.nutrifit.app.ui;

import android.view.View;
import com.nutrifit.app.R;

final class PremiumScreen {
  PremiumScreen(MainActivity a) {
    View root = a.layout(R.layout.screen_premium);
    boolean active = a.repo.premium();
    Ui.text(
        root,
        R.id.premium_status,
        a.getString(active ? R.string.premium_active : R.string.premium_inactive));
    Ui.text(
        root,
        R.id.premium_toggle,
        a.getString(active ? R.string.premium_disable : R.string.premium_enable));
    root.findViewById(R.id.premium_toggle)
        .setOnClickListener(
            v ->
                a.write(
                    () -> a.repo.premium(!active),
                    () -> a.show(active ? R.id.nav_premium : R.id.nav_plan)));
  }
}
