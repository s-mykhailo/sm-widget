package com.sm.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Будує RemoteViews для віджета (і для прев'ю в редакторі). */
public class Render {
    static final int[] RADIUS = {R.drawable.bg_r0, R.drawable.bg_r1, R.drawable.bg_r2, R.drawable.bg_r3, R.drawable.bg_r4};
    static final float[] SCALE = {0.8f, 1f, 1.25f, 1.55f};
    static final String[] MONTHS = {"Січень", "Лютий", "Березень", "Квітень", "Травень", "Червень", "Липень",
            "Серпень", "Вересень", "Жовтень", "Листопад", "Грудень"};

    public static Class<?> provider(String type) {
        switch (type) {
            case "photo": return PhotoWidget.class;
            case "text": return TextWidget.class;
            case "battery": return BatteryWidget.class;
            case "progress": return ProgressWidget.class;
            case "countdown": return CountdownWidget.class;
            default: return ClockWidget.class;
        }
    }

    public static String title(String type) {
        switch (type) {
            case "photo": return "Фото";
            case "text": return "Текст";
            case "battery": return "Батарея";
            case "progress": return "Прогрес";
            case "countdown": return "Відлік";
            default: return "Годинник";
        }
    }

    public static String typeOf(String className) {
        for (String t : Cfg.TYPES) if (provider(t).getName().equals(className)) return t;
        return "clock";
    }

    public static void updateAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        for (String t : Cfg.TYPES) {
            if (t.equals("photo")) continue;
            for (int id : m.getAppWidgetIds(new ComponentName(c, provider(t)))) {
                m.updateAppWidget(id, build(c, Cfg.load(c, id, t), id));
            }
        }
    }

    static String plural(long n) {
        long a = Math.abs(n) % 100, b = a % 10;
        if (a > 10 && a < 20) return "днів";
        if (b == 1) return "день";
        if (b >= 2 && b <= 4) return "дні";
        return "днів";
    }

    public static RemoteViews build(Context c, Cfg cfg, int widgetId) {
        if (cfg.type.equals("photo")) {
            RemoteViews rv = Photos.remote(c, cfg, widgetId, widgetId > 0 ? 900 : 300, null);
            click(c, rv, cfg, widgetId);
            return rv;
        }
        String al = cfg.align == 0 ? "l" : cfg.align == 2 ? "r" : "c";
        int layout = c.getResources().getIdentifier("w_" + cfg.type + "_" + cfg.font + "_" + al, "layout", c.getPackageName());
        if (layout == 0) layout = c.getResources().getIdentifier("w_" + cfg.type + "_sans_c", "layout", c.getPackageName());
        RemoteViews rv = new RemoteViews(c.getPackageName(), layout);

        int bg = cfg.bgColor(c), fg = cfg.fgColor(c);
        rv.setImageViewResource(R.id.bg, RADIUS[Math.max(0, Math.min(4, cfg.radius))]);
        rv.setInt(R.id.bg, "setColorFilter", bg);
        rv.setInt(R.id.bg, "setImageAlpha", Math.round(cfg.alpha * 2.55f));

        rv.setTextColor(R.id.line1, fg);
        rv.setTextColor(R.id.line2, fg);
        float s = SCALE[Math.max(0, Math.min(3, cfg.size))];
        int sp = TypedValue.COMPLEX_UNIT_SP;

        switch (cfg.type) {
            case "clock": {
                rv.setTextViewTextSize(R.id.line1, sp, 56 * s);
                rv.setTextViewTextSize(R.id.line2, sp, (cfg.mode == 2 ? 30 : 16) * s);
                rv.setViewVisibility(R.id.line1, cfg.mode == 2 ? View.GONE : View.VISIBLE);
                rv.setViewVisibility(R.id.line2, cfg.mode == 1 ? View.GONE : View.VISIBLE);
                break;
            }
            case "text": {
                rv.setTextViewText(R.id.line1, cfg.text1.isEmpty() ? " " : cfg.text1);
                rv.setTextViewText(R.id.line2, cfg.text2);
                rv.setTextViewTextSize(R.id.line1, sp, 22 * s);
                rv.setTextViewTextSize(R.id.line2, sp, 13 * s);
                rv.setViewVisibility(R.id.line2, cfg.text2.trim().isEmpty() ? View.GONE : View.VISIBLE);
                break;
            }
            case "battery": {
                BatteryManager bm = c.getSystemService(BatteryManager.class);
                int lvl = bm == null ? 0 : bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                boolean ch = bm != null && bm.isCharging();
                rv.setImageViewBitmap(R.id.image, ring(lvl / 100f, fg));
                rv.setTextViewText(R.id.line1, lvl + "%");
                rv.setTextViewText(R.id.line2, ch ? "⚡ Заряджається" : (cfg.text1.trim().isEmpty() ? "Батарея" : cfg.text1));
                rv.setTextViewTextSize(R.id.line1, sp, 22 * s);
                rv.setTextViewTextSize(R.id.line2, sp, 12 * s);
                break;
            }
            case "progress": {
                LocalDateTime now = LocalDateTime.now();
                double sec = now.toLocalTime().toSecondOfDay(), day = sec / 86400.0, f;
                String label, left;
                LocalDate d = now.toLocalDate();
                switch (cfg.mode) {
                    case 0: f = day; label = "Сьогодні"; left = "залишилось " + (int) ((86400 - sec) / 3600) + " год"; break;
                    case 1: { int dw = d.getDayOfWeek().getValue(); f = (dw - 1 + day) / 7; label = "Тиждень";
                        int l = 7 - dw; left = "залишилось " + l + " " + plural(l); break; }
                    case 2: { int len = d.lengthOfMonth(); f = (d.getDayOfMonth() - 1 + day) / len; label = MONTHS[d.getMonthValue() - 1];
                        int l = len - d.getDayOfMonth(); left = "залишилось " + l + " " + plural(l); break; }
                    default: { int len = d.lengthOfYear(); f = (d.getDayOfYear() - 1 + day) / len; label = "Рік " + d.getYear();
                        int l = len - d.getDayOfYear(); left = "залишилось " + l + " " + plural(l); break; }
                }
                if (!cfg.text1.trim().isEmpty()) label = cfg.text1;
                rv.setTextViewText(R.id.line1, label);
                rv.setTextViewText(R.id.line2, (int) Math.floor(f * 100) + "% · " + left);
                rv.setProgressBar(R.id.bar, 1000, (int) (f * 1000), false);
                if (Build.VERSION.SDK_INT >= 31) {
                    rv.setColorStateList(R.id.bar, "setProgressTintList", ColorStateList.valueOf(fg));
                    rv.setColorStateList(R.id.bar, "setProgressBackgroundTintList", ColorStateList.valueOf((fg & 0x00FFFFFF) | 0x40000000));
                }
                rv.setTextViewTextSize(R.id.line1, sp, 16 * s);
                rv.setTextViewTextSize(R.id.line2, sp, 13 * s);
                break;
            }
            case "countdown": {
                LocalDate today = LocalDate.now();
                LocalDate t = LocalDate.ofEpochDay(cfg.date);
                if (cfg.yearly) while (t.isBefore(today)) t = t.plusYears(1);
                long n = t.toEpochDay() - today.toEpochDay();
                String what = cfg.text1.trim();
                if (n == 0) {
                    rv.setTextViewText(R.id.line1, "Сьогодні!");
                    rv.setTextViewText(R.id.line2, what);
                    rv.setTextViewTextSize(R.id.line1, sp, 30 * s);
                } else if (n > 0) {
                    rv.setTextViewText(R.id.line1, String.valueOf(n));
                    rv.setTextViewText(R.id.line2, plural(n) + (what.isEmpty() ? " залишилось" : " до " + what));
                    rv.setTextViewTextSize(R.id.line1, sp, 48 * s);
                } else {
                    rv.setTextViewText(R.id.line1, String.valueOf(-n));
                    rv.setTextViewText(R.id.line2, plural(n) + " тому" + (what.isEmpty() ? "" : " · " + what));
                    rv.setTextViewTextSize(R.id.line1, sp, 48 * s);
                }
                rv.setTextViewTextSize(R.id.line2, sp, 13 * s);
                break;
            }
        }

        click(c, rv, cfg, widgetId);
        return rv;
    }

    static void click(Context c, RemoteViews rv, Cfg cfg, int widgetId) {
        if (widgetId <= 0) return;
        if (cfg.type.equals("photo") && cfg.tap == 0 && cfg.photos.size() > 1) {
            Intent n = new Intent(c, PhotoWidget.class).setAction(PhotoWidget.NEXT)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    .setData(Uri.parse("smw://next/" + widgetId));
            rv.setOnClickPendingIntent(R.id.root, PendingIntent.getBroadcast(c, widgetId, n,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            return;
        }
        {
            Intent i = new Intent(c, ConfigActivity.class)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    .setData(Uri.parse("smw://edit/" + widgetId))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            rv.setOnClickPendingIntent(R.id.root, PendingIntent.getActivity(c, widgetId, i,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        }
    }

    static Bitmap ring(float f, int color) {
        int n = 240; float w = 22;
        Bitmap b = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(w); p.setStrokeCap(Paint.Cap.ROUND);
        RectF r = new RectF(w, w, n - w, n - w);
        p.setColor((color & 0x00FFFFFF) | 0x40000000);
        cv.drawArc(r, 0, 360, false, p);
        p.setColor(color);
        cv.drawArc(r, -90, Math.max(1f, 360 * f), false, p);
        return b;
    }
}
