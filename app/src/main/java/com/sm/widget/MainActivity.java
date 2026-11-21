package com.sm.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    Ui ui;
    LinearLayout mine;

    static final String[] DESC = {
            "Твоє фото у формі серця, кола, арки... Можна кілька - будуть гортатись",
            "Час і дата, які оновлюються самі",
            "Цитата, ім'я, нагадування - будь-що",
            "Кільце заряду з відсотками",
            "Скільки пройшло від дня, тижня, місяця, року",
            "Скільки днів до події"};

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        ui = new Ui(this);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(ui.background);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int pad = ui.dp(18);
        col.setPadding(pad, pad, pad, pad);
        sv.addView(col);
        sv.setOnApplyWindowInsetsListener((v, ins) -> {
            android.graphics.Insets s = ins.getInsets(WindowInsets.Type.systemBars());
            v.setPadding(s.left, s.top, s.right, s.bottom);
            return WindowInsets.CONSUMED;
        });
        setContentView(sv);

        col.addView(ui.text("SM Widget", 34, ui.onSurface, true));
        TextView sub = ui.text("Декоративні віджети для робочого столу.\nБез інтернету. Без реклами.", 15, ui.muted, false);
        sub.setPadding(0, ui.dp(4), 0, ui.dp(6));
        col.addView(sub);

        mine = new LinearLayout(this);
        mine.setOrientation(LinearLayout.VERTICAL);
        col.addView(mine);

        col.addView(ui.header("Створити віджет"));
        for (int i = 0; i < Cfg.TYPES.length; i++) col.addView(card(Cfg.TYPES[i], DESC[i]));

        TextView tip = ui.text("Порада: віджет можна додати й так - довго натисни на робочий стіл → Віджети → SM Widget. "
                + "Щоб змінити віджет, просто торкнись його на екрані.", 13, ui.muted, false);
        tip.setPadding(ui.dp(4), ui.dp(16), ui.dp(4), ui.dp(8));
        col.addView(tip);
    }

    View card(String type, String desc) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(ui.round(ui.surface, 24));
        int p = ui.dp(16);
        card.setPadding(p, p, p, p);
        LinearLayout.LayoutParams l = ui.lp(Ui.MP, Ui.WC); l.bottomMargin = ui.dp(12);
        card.setLayoutParams(l);

        FrameLayout stage = new FrameLayout(this);
        GradientDrawable wall = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF3A4A6B, 0xFF6B4A6B, 0xFF2D5B5B});
        wall.setCornerRadius(ui.dp(16));
        stage.setBackground(wall);
        stage.setPadding(ui.dp(12), ui.dp(12), ui.dp(12), ui.dp(12));
        int[] sz = Ui.previewSize(type);
        FrameLayout.LayoutParams wl = new FrameLayout.LayoutParams(type.equals("battery") || type.equals("countdown") || type.equals("photo") ? ui.dp(sz[0] * 0.8f) : Ui.MP, ui.dp(sz[1] * 0.8f));
        wl.gravity = Gravity.CENTER;
        try {
            Cfg d = Cfg.def(type);
            if (type.equals("photo")) d.shape = 3;
            View w = Render.build(this, d, -1).apply(this, stage);
            stage.addView(w, wl);
        } catch (Exception ignored) {}
        card.addView(stage, ui.lp(Ui.MP, Ui.WC));

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(ui.dp(4), ui.dp(12), 0, 0);
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(ui.text(Render.title(type), 18, ui.onSurface, true));
        texts.addView(ui.text(desc, 13, ui.muted, false));
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, Ui.WC, 1); tl.rightMargin = ui.dp(8);
        row.addView(texts, tl);
        TextView btn = ui.button("Створити", true);
        btn.setOnClickListener(v -> startActivity(new Intent(this, ConfigActivity.class).putExtra("type", type)));
        row.addView(btn);
        card.addView(row);
        return card;
    }

    @Override protected void onResume() {
        super.onResume();
        Render.updateAll(this);
        new Thread(() -> Photos.gc(this)).start();
        mine.removeAllViews();
        AppWidgetManager m = AppWidgetManager.getInstance(this);
        boolean any = false;
        for (String t : Cfg.TYPES) {
            for (int id : m.getAppWidgetIds(new ComponentName(this, Render.provider(t)))) {
                if (!any) { mine.addView(ui.header("На робочому столі")); any = true; }
                LinearLayout r = new LinearLayout(this);
                r.setGravity(Gravity.CENTER_VERTICAL);
                r.setBackground(ui.round(ui.surface, 18));
                r.setPadding(ui.dp(16), ui.dp(10), ui.dp(10), ui.dp(10));
                LinearLayout.LayoutParams l = ui.lp(Ui.MP, Ui.WC); l.bottomMargin = ui.dp(8);
                Cfg cfg = Cfg.load(this, id, t);
                String extra = t.equals("photo") ? " · " + cfg.photos.size() + " фото" : t.equals("text") || t.equals("countdown") ? (cfg.text1.isEmpty() ? "" : " · " + cfg.text1) : "";
                TextView name = ui.text(Render.title(t) + extra, 15, ui.onSurface, false);
                name.setSingleLine(true);
                name.setEllipsize(android.text.TextUtils.TruncateAt.END);
                r.addView(name, new LinearLayout.LayoutParams(0, Ui.WC, 1));
                TextView e = ui.button("Змінити", false);
                e.setPadding(ui.dp(16), ui.dp(8), ui.dp(16), ui.dp(8));
                e.setOnClickListener(v -> startActivity(new Intent(this, ConfigActivity.class)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)));
                r.addView(e);
                mine.addView(r, l);
            }
        }
    }
}
