package com.sm.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/** Дрібні хелпери для програмного інтерфейсу. */
public class Ui {
    final Context c;
    final boolean night;
    final int surface, surface2, onSurface, muted, accent, onAccent, background;

    Ui(Context c) {
        this.c = c;
        night = (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        boolean d = Build.VERSION.SDK_INT >= 31;
        if (d) {
            background = c.getColor(night ? android.R.color.system_neutral1_900 : android.R.color.system_neutral1_10);
            surface = c.getColor(night ? android.R.color.system_neutral1_800 : android.R.color.system_neutral1_50);
            surface2 = c.getColor(night ? android.R.color.system_neutral2_700 : android.R.color.system_neutral2_100);
            accent = c.getColor(night ? android.R.color.system_accent1_200 : android.R.color.system_accent1_600);
            onAccent = c.getColor(night ? android.R.color.system_accent1_800 : android.R.color.system_accent1_0);
        } else {
            background = night ? 0xFF121212 : 0xFFFFFFFF;
            surface = night ? 0xFF1F1F1F : 0xFFF1F3F4;
            surface2 = night ? 0xFF3C4043 : 0xFFE0E3E7;
            accent = night ? 0xFF8AB4F8 : 0xFF1A73E8;
            onAccent = night ? 0xFF062E6F : 0xFFFFFFFF;
        }
        onSurface = night ? 0xFFE8EAED : 0xFF1F1F1F;
        muted = night ? 0xFFB0B4BA : 0xFF5F6368;
    }

    int dp(float v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }

    GradientDrawable round(int color, float r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(r));
        return g;
    }

    TextView text(String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return t;
    }

    TextView header(String s) {
        TextView t = text(s, 14, muted, true);
        t.setPadding(dp(4), dp(18), 0, dp(8));
        return t;
    }

    TextView button(String s, boolean filled) {
        TextView b = text(s, 15, filled ? onAccent : accent, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(20), dp(12), dp(20), dp(12));
        GradientDrawable g = round(filled ? accent : Color.TRANSPARENT, 100);
        if (!filled) g.setStroke(dp(1), accent);
        b.setBackground(g);
        b.setClickable(true);
        return b;
    }

    LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w, h); }
    static final int MP = ViewGroup.LayoutParams.MATCH_PARENT, WC = ViewGroup.LayoutParams.WRAP_CONTENT;

    interface Pick { void on(int i); }

    /** Ряд «пігулок» з одним вибраним. */
    View chips(String[] labels, Typeface[] faces, int selected, Pick pick) {
        HorizontalScrollView hs = new HorizontalScrollView(c);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(c);
        hs.addView(row);
        List<TextView> items = new ArrayList<>();
        final int[] sel = {selected};
        for (int i = 0; i < labels.length; i++) {
            TextView t = text(labels[i], 14, onSurface, false);
            if (faces != null) t.setTypeface(faces[i]);
            t.setPadding(dp(16), dp(9), dp(16), dp(9));
            LinearLayout.LayoutParams l = lp(WC, WC); l.rightMargin = dp(8);
            row.addView(t, l);
            items.add(t);
            final int idx = i;
            t.setOnClickListener(v -> { sel[0] = idx; style(items, idx); pick.on(idx); });
        }
        if (sel[0] >= 0) style(items, sel[0]); else style(items, -1);
        return hs;
    }

    void style(List<TextView> items, int sel) {
        for (int i = 0; i < items.size(); i++) {
            TextView t = items.get(i);
            boolean on = i == sel;
            t.setBackground(round(on ? accent : surface2, 100));
            t.setTextColor(on ? onAccent : onSurface);
        }
    }

    /** Ряд кольорових кружечків. values: ARGB або Cfg.DYN1/DYN2, -100 = власний hex. */
    View swatches(int[] values, int[] shown, int selected, Pick pick) {
        HorizontalScrollView hs = new HorizontalScrollView(c);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(c);
        row.setPadding(0, dp(2), 0, dp(2));
        hs.addView(row);
        List<TextView> items = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            TextView t = new TextView(c);
            t.setGravity(Gravity.CENTER);
            t.setTextSize(12);
            int col = shown[i];
            float lum = (Color.red(col) * 0.299f + Color.green(col) * 0.587f + Color.blue(col) * 0.114f);
            t.setTextColor(lum > 150 ? Color.BLACK : Color.WHITE);
            if (values[i] == Cfg.DYN1 || values[i] == Cfg.DYN2) t.setText("You");
            if (values[i] == -100) { t.setText("#"); t.setTextSize(18); t.setTextColor(onSurface); }
            t.setTag(col);
            LinearLayout.LayoutParams l = lp(dp(44), dp(44)); l.rightMargin = dp(8);
            row.addView(t, l);
            items.add(t);
            final int idx = i;
            t.setOnClickListener(v -> { if (values[idx] != -100) markSw(items, values, idx); pick.on(idx); });
        }
        int s = -1;
        for (int i = 0; i < values.length; i++) if (values[i] == selected) s = i;
        markSw(items, values, s);
        return hs;
    }

    void markSw(List<TextView> items, int[] values, int sel) {
        for (int i = 0; i < items.size(); i++) {
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            int col = (int) items.get(i).getTag();
            g.setColor(values[i] == -100 ? surface2 : col);
            if (i == sel) g.setStroke(dp(3), accent);
            else g.setStroke(dp(1), (onSurface & 0x00FFFFFF) | 0x33000000);
            items.get(i).setBackground(g);
        }
    }

    static Typeface[] fontFaces() {
        Typeface[] f = new Typeface[Cfg.FONT_FAMILIES.length];
        for (int i = 0; i < f.length; i++) f[i] = Typeface.create(Cfg.FONT_FAMILIES[i], Typeface.NORMAL);
        return f;
    }

    /** Розмір прев'ю віджета (dp) за типом. */
    static int[] previewSize(String type) {
        switch (type) {
            case "photo": return new int[]{170, 170};
            case "battery": case "countdown": return new int[]{160, 160};
            case "text": case "progress": return new int[]{320, 100};
            default: return new int[]{320, 150};
        }
    }
}
