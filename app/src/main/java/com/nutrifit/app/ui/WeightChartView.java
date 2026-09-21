package com.nutrifit.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import com.nutrifit.app.data.AppDatabase;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public class WeightChartView extends View {
  public WeightChartView(Context context, android.util.AttributeSet attrs) {
    this(context, java.util.Collections.emptyList());
  }

  private final List<AppDatabase.Weight> data;
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final long[] days;
  private final double min, max;

  public WeightChartView(Context c, List<AppDatabase.Weight> data) {
    super(c);
    this.data = data;
    days = new long[data.size()];
    for (int i = 0; i < data.size(); i++) days[i] = LocalDate.parse(data.get(i).day).toEpochDay();
    min = data.stream().mapToDouble(w -> w.kg).min().orElse(0) - 1;
    max = data.stream().mapToDouble(w -> w.kg).max().orElse(1) + 1;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (data.isEmpty()) return;
    float d = getResources().getDisplayMetrics().density, left = 55 * d, top = 25 * d;
    float right = getWidth() - 20 * d, bottom = getHeight() - 35 * d;
    paint.setTextSize(12 * d);
    paint.setStrokeWidth(2 * d);
    paint.setColor(Color.GRAY);
    canvas.drawLine(left, top, left, bottom, paint);
    canvas.drawLine(left, bottom, right, bottom, paint);
    canvas.drawText(String.format(Locale.getDefault(), "%.1f", max), 0, top, paint);
    canvas.drawText(String.format(Locale.getDefault(), "%.1f", min), 0, bottom, paint);
    long start = days[0];
    long end = days[days.length - 1];
    canvas.drawText(data.get(0).day.substring(5), left, bottom + 22 * d, paint);
    if (data.size() > 1)
      canvas.drawText(
          data.get(data.size() - 1).day.substring(5), right - 40 * d, bottom + 22 * d, paint);
    float px = 0, py = 0;
    paint.setColor(Color.rgb(22, 134, 106));
    for (int i = 0; i < data.size(); i++) {
      AppDatabase.Weight w = data.get(i);
      float x = left + (right - left) * (days[i] - start) / Math.max(1, end - start);
      float y = bottom - (float) ((w.kg - min) / (max - min)) * (bottom - top);
      if (i > 0) canvas.drawLine(px, py, x, y, paint);
      canvas.drawCircle(x, y, 4 * d, paint);
      px = x;
      py = y;
    }
  }
}
