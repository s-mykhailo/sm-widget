package com.sm.widget;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.ImageView;
import java.util.ArrayList;
import java.util.List;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ConfigActivity extends Activity {
    Ui ui;
    Cfg cfg;
    String type;
    int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    FrameLayout stage;
    LinearLayout body;
    Bitmap previewSrc;
    String previewName;
    LinearLayout thumbs;
    static final int PICK = 7;
    int thumbGen = 0;
    CheckBox applyAll;

    static final int[] PALETTE = {0xFF000000, 0xFF202124, 0xFF3C4043, 0xFFFFFFFF, 0xFFF1F3F4, 0xFF8AB4F8, 0xFF1A73E8,
            0xFF78D9EC, 0xFF81C995, 0xFF188038, 0xFFFDD663, 0xFFFCAD70, 0xFFF28B82, 0xFFD93025, 0xFFFF8BCB, 0xFFC58AF9, 0xFF7E57C2, 0xFFA1887F};

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        ui = new Ui(this);
        Intent in = getIntent();
        widgetId = in.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_CANCELED, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId));
            AppWidgetProviderInfo info = AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId);
            if (info == null) { finish(); return; }
            type = Render.typeOf(info.provider.getClassName());
            cfg = Cfg.load(this, widgetId, type);
        } else {
            type = in.getStringExtra("type");
            if (type == null) type = "clock";
            cfg = Cfg.def(type);
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ui.background);
        root.setOnApplyWindowInsetsListener((v, ins) -> {
            android.graphics.Insets s = ins.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
            v.setPadding(s.left, s.top, s.right, s.bottom);
            return WindowInsets.CONSUMED;
        });
        setContentView(root);

        // заголовок
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(ui.dp(8), ui.dp(8), ui.dp(16), ui.dp(4));
        TextView back = ui.text("←", 24, ui.onSurface, false);
        back.setPadding(ui.dp(12), ui.dp(4), ui.dp(16), ui.dp(4));
        back.setOnClickListener(v -> finish());
        top.addView(back);
        top.addView(ui.text(Render.title(type), 22, ui.onSurface, true));
        root.addView(top);

        // прев'ю
        stage = new FrameLayout(this);
        GradientDrawable wall = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF3A4A6B, 0xFF6B4A6B, 0xFF2D5B5B});
        wall.setCornerRadius(ui.dp(24));
        stage.setBackground(wall);
        LinearLayout.LayoutParams sl = ui.lp(Ui.MP, ui.dp(type.equals("photo") ? 230 : Ui.previewSize(type)[1] + 40));
        sl.leftMargin = sl.rightMargin = ui.dp(16); sl.topMargin = ui.dp(4);
        root.addView(stage, sl);

        // налаштування
        ScrollView sv = new ScrollView(this);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(ui.dp(16), 0, ui.dp(16), ui.dp(16));
        sv.addView(body);
        root.addView(sv, new LinearLayout.LayoutParams(Ui.MP, 0, 1));

        buildOptions();

        TextView save = ui.button(widgetId > 0 ? "Зберегти" : "Поставити на екран", true);
        save.setOnClickListener(v -> save());
        LinearLayout.LayoutParams bl = ui.lp(Ui.MP, Ui.WC);
        bl.setMargins(ui.dp(16), ui.dp(8), ui.dp(16), ui.dp(12));
        root.addView(save, bl);

        refresh();
    }

    void refresh() {
        stage.removeAllViews();
        if (type.equals("photo")) {
            String want = cfg.photos.isEmpty() ? null : cfg.photos.get(Math.floorMod(cfg.idx, cfg.photos.size()));
            if (want == null || !want.equals(previewName)) {
                if (previewSrc != null) previewSrc.recycle();
                previewSrc = want == null ? null : Photos.load(this, want, 700, 700);
                previewName = want;
            }
            float[] dp = Photos.sizeDp(this, widgetId);
            float k = Math.min(300f / dp[0], 200f / dp[1]);
            k = Math.min(k, 1.25f);
            FrameLayout.LayoutParams l = new FrameLayout.LayoutParams(ui.dp(dp[0] * k), ui.dp(dp[1] * k));
            l.gravity = Gravity.CENTER;
            Cfg pc = cfg.copy();
            try {
                ImageView iv = new ImageView(this);
                float dens = getResources().getDisplayMetrics().density;
                int pw = ui.dp(dp[0] * k), ph = ui.dp(dp[1] * k);
                iv.setImageBitmap(Photos.draw(this, pc, previewSrc, pw, ph, dens * k));
                stage.addView(iv, l);
            } catch (Exception e) { stage.addView(ui.text("Помилка прев'ю", 14, Color.WHITE, false)); }
            return;
        }
        int[] sz = Ui.previewSize(type);
        boolean square = type.equals("battery") || type.equals("countdown");
        FrameLayout.LayoutParams l = new FrameLayout.LayoutParams(square ? ui.dp(sz[0]) : Ui.MP, ui.dp(sz[1]));
        l.gravity = Gravity.CENTER;
        l.leftMargin = l.rightMargin = ui.dp(16);
        try { stage.addView(Render.build(this, cfg, -1).apply(this, stage), l); }
        catch (Exception e) { stage.addView(ui.text("Помилка прев'ю", 14, Color.WHITE, false)); }
    }

    void section(String title, View v) {
        body.addView(ui.header(title));
        body.addView(v);
    }

    EditText input(String hint, String value, boolean multi, TextWatcherLite w) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value);
        e.setTextColor(ui.onSurface);
        e.setHintTextColor(ui.muted);
        e.setBackground(ui.round(ui.surface, 16));
        e.setPadding(ui.dp(16), ui.dp(12), ui.dp(16), ui.dp(12));
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | (multi ? InputType.TYPE_TEXT_FLAG_MULTI_LINE : 0));
        e.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(Editable s) { w.on(s.toString()); refresh(); }
        });
        return e;
    }
    interface TextWatcherLite { void on(String s); }

    void buildOptions() {
        if (type.equals("photo")) { buildPhotoOptions(); return; }
        switch (type) {
            case "clock":
                section("Що показувати", ui.chips(new String[]{"Час і дата", "Тільки час", "Тільки дата"}, null, cfg.mode,
                        i -> { cfg.mode = i; refresh(); }));
                break;
            case "text":
                section("Текст", input("Напиши щось…", cfg.text1, true, s -> cfg.text1 = s));
                section("Підпис (необов'язково)", input("Наприклад, автор цитати", cfg.text2, false, s -> cfg.text2 = s));
                break;
            case "battery":
                section("Підпис", input("Батарея", cfg.text1, false, s -> cfg.text1 = s));
                break;
            case "progress":
                section("Період", ui.chips(new String[]{"День", "Тиждень", "Місяць", "Рік"}, null, cfg.mode,
                        i -> { cfg.mode = i; refresh(); }));
                section("Власна назва (необов'язково)", input("Наприклад, Мій 2026", cfg.text1, false, s -> cfg.text1 = s));
                break;
            case "countdown": {
                TextView d = ui.button("", false);
                Runnable setLabel = () -> d.setText("Дата: " + LocalDate.ofEpochDay(cfg.date).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
                setLabel.run();
                d.setOnClickListener(v -> {
                    LocalDate x = LocalDate.ofEpochDay(cfg.date);
                    new DatePickerDialog(this, (dp, y, m, day) -> {
                        cfg.date = LocalDate.of(y, m + 1, day).toEpochDay(); setLabel.run(); refresh();
                    }, x.getYear(), x.getMonthValue() - 1, x.getDayOfMonth()).show();
                });
                section("Подія", d);
                section("До чого (у формі «до …»)", input("Наприклад, Нового року", cfg.text1, false, s -> cfg.text1 = s));
                CheckBox cb = new CheckBox(this);
                cb.setText("Повторювати щороку (день народження, свята)");
                cb.setTextColor(ui.onSurface);
                cb.setChecked(cfg.yearly);
                cb.setOnCheckedChangeListener((v, on) -> { cfg.yearly = on; refresh(); });
                cb.setPadding(ui.dp(4), ui.dp(8), 0, 0);
                body.addView(cb);
                break;
            }
        }

        section("Шрифт", ui.chips(Cfg.FONT_NAMES, Ui.fontFaces(), cfg.fontIndex(),
                i -> { cfg.font = Cfg.FONT_KEYS[i]; refresh(); }));
        section("Колір фону", colorRow(true));

        SeekBar sb = new SeekBar(this);
        sb.setMax(100);
        sb.setProgress(cfg.alpha);
        TextView alphaLabel = ui.header("Непрозорість фону: " + cfg.alpha + "%");
        body.addView(alphaLabel);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) { cfg.alpha = p; alphaLabel.setText("Непрозорість фону: " + p + "%"); refresh(); }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        body.addView(sb);

        section("Кути", ui.chips(new String[]{"Гострі", "Малі", "Середні", "Великі", "Максимальні"}, null, cfg.radius,
                i -> { cfg.radius = i; refresh(); }));
        section("Колір тексту", colorRow(false));
        section("Розмір тексту", ui.chips(new String[]{"S", "M", "L", "XL"}, null, cfg.size, i -> { cfg.size = i; refresh(); }));
        section("Вирівнювання", ui.chips(new String[]{"Ліворуч", "По центру", "Праворуч"}, null, cfg.align,
                i -> { cfg.align = i; refresh(); }));
    }

    View colorRow(boolean isBg) {
        boolean dyn = android.os.Build.VERSION.SDK_INT >= 31;
        int extra = dyn ? 2 : 0;
        int[] values = new int[PALETTE.length + extra + 1];
        int[] shown = new int[values.length];
        int k = 0;
        if (dyn) {
            Cfg probe = new Cfg();
            probe.bg = Cfg.DYN1; probe.fg = Cfg.DYN1;
            values[k] = Cfg.DYN1; shown[k++] = isBg ? probe.bgColor(this) : probe.fgColor(this);
            probe.bg = Cfg.DYN2; probe.fg = Cfg.DYN2;
            values[k] = Cfg.DYN2; shown[k++] = isBg ? probe.bgColor(this) : probe.fgColor(this);
        }
        for (int c : PALETTE) { values[k] = c; shown[k++] = c; }
        values[k] = -100; shown[k] = 0;
        int current = isBg ? cfg.bg : cfg.fg;
        return ui.swatches(values, shown, current, i -> {
            if (values[i] == -100) { hexDialog(isBg); return; }
            if (isBg) cfg.bg = values[i]; else cfg.fg = values[i];
            refresh();
        });
    }

    void hexDialog(boolean isBg) {
        EditText e = new EditText(this);
        e.setHint("#FF5722");
        int cur = isBg ? cfg.bgColor(this) : cfg.fgColor(this);
        e.setText(String.format("#%06X", cur & 0xFFFFFF));
        new AlertDialog.Builder(this).setTitle("Свій колір (HEX)").setView(e)
                .setPositiveButton("OK", (d, w) -> {
                    try {
                        String s = e.getText().toString().trim();
                        if (!s.startsWith("#")) s = "#" + s;
                        int c = Color.parseColor(s) | 0xFF000000;
                        if (isBg) cfg.bg = c; else cfg.fg = c;
                        refresh();
                    } catch (Exception ex) { Toast.makeText(this, "Невірний колір", Toast.LENGTH_SHORT).show(); }
                })
                .setNegativeButton("Скасувати", null).show();
    }

    void buildPhotoOptions() {
        body.addView(ui.header("Фото (до 30 штук, тисни на фото щоб показати/прибрати)"));
        android.widget.HorizontalScrollView hs = new android.widget.HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        thumbs = new LinearLayout(this);
        hs.addView(thumbs);
        body.addView(hs);
        fillThumbs();

        section("Форма", ui.chips(Photos.SHAPES, null, cfg.shape, i -> { cfg.shape = i; refresh(); }));
        section("Кути (для форми «Прямокутник»)", ui.chips(new String[]{"Гострі", "Малі", "Середні", "Великі", "Максимальні"}, null, cfg.radius,
                i -> { cfg.radius = i; refresh(); }));
        section("Рамка", ui.chips(new String[]{"Без рамки", "Тонка", "Товста"}, null, cfg.border, i -> { cfg.border = i; refresh(); }));
        section("Колір рамки", colorRow(false));
        section("Фільтр", ui.chips(Photos.FILTERS, null, cfg.filter, i -> { cfg.filter = i; refresh(); }));
        TextView opLabel = ui.header("Непрозорість фото: " + cfg.opacity + "%");
        body.addView(opLabel);
        SeekBar ob = new SeekBar(this);
        ob.setMin(10);
        ob.setMax(100);
        ob.setProgress(cfg.opacity);
        ob.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) { cfg.opacity = p; opLabel.setText("Непрозорість фото: " + p + "%"); refresh(); }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        body.addView(ob);
        TextView q = ui.text("Швидко:", 12, ui.muted, false);
        q.setPadding(ui.dp(4), ui.dp(4), 0, ui.dp(4));
        body.addView(q);
        int[] presets = {100, 85, 70, 55};
        body.addView(ui.chips(new String[]{"100%", "85% - ледь", "70% - легкий", "55% - сильний"}, null, -1, i -> {
            cfg.opacity = presets[i]; ob.setProgress(presets[i]); refresh();
        }));
        section("Скляний ефект", ui.chips(new String[]{"Вимк", "Легкий", "Сильний"}, null, cfg.glass, i -> { cfg.glass = i; refresh(); }));
        applyAll = new CheckBox(this);
        applyAll.setText("Застосувати прозорість і скло до всіх моїх фото-віджетів");
        applyAll.setTextColor(ui.onSurface);
        applyAll.setPadding(ui.dp(4), ui.dp(8), 0, 0);
        body.addView(applyAll);

        section("Підпис на фото (необов'язково)", input("Наприклад, літо 2026", cfg.text1, false, s -> cfg.text1 = s));
        section("Шрифт підпису", ui.chips(Cfg.FONT_NAMES, Ui.fontFaces(), cfg.fontIndex(),
                i -> { cfg.font = Cfg.FONT_KEYS[i]; refresh(); }));
        section("Розмір підпису", ui.chips(new String[]{"S", "M", "L", "XL"}, null, cfg.size, i -> { cfg.size = i; refresh(); }));
        section("Якщо фото кілька - міняти", ui.chips(new String[]{"Тільки по дотику", "Кожні 30 хв", "Раз на день"}, null, cfg.every,
                i -> cfg.every = i));
        section("Дотик до віджета", ui.chips(new String[]{"Наступне фото", "Відкрити налаштування"}, null, cfg.tap, i -> cfg.tap = i));
        TextView hint = ui.text("Якщо дотик гортає фото, налаштування відкриваються так: довго натисни на віджет → «Налаштувати», або через застосунок SM Widget.",
                12, ui.muted, false);
        hint.setPadding(ui.dp(4), ui.dp(10), ui.dp(4), 0);
        body.addView(hint);
    }

    void fillThumbs() {
        thumbs.removeAllViews();
        TextView add = ui.text("+\nДодати", 13, ui.accent, true);
        add.setGravity(Gravity.CENTER);
        add.setBackground(ui.round(ui.surface, 16));
        LinearLayout.LayoutParams al = ui.lp(ui.dp(72), ui.dp(72)); al.rightMargin = ui.dp(8);
        add.setOnClickListener(v -> pick());
        thumbs.addView(add, al);
        List<String> names = new ArrayList<>(cfg.photos);
        final int gen = ++thumbGen;
        new Thread(() -> {
            for (String n : names) {
                Bitmap t = Photos.load(this, n, ui.dp(72), ui.dp(72));
                if (gen != thumbGen) return;
                runOnUiThread(() -> {
                    if (gen != thumbGen) return;
                    ImageView iv = new ImageView(this);
                    iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    iv.setImageBitmap(t);
                    iv.setClipToOutline(true);
                    iv.setBackground(ui.round(ui.surface, 16));
                    LinearLayout.LayoutParams l = ui.lp(ui.dp(72), ui.dp(72)); l.rightMargin = ui.dp(8);
                    iv.setOnClickListener(v -> photoMenu(n));
                    thumbs.addView(iv, l);
                });
            }
        }).start();
    }

    void photoMenu(String name) {
        new AlertDialog.Builder(this).setItems(new String[]{"Показати в прев'ю", "Прибрати це фото"}, (d, w) -> {
            int i = cfg.photos.indexOf(name);
            if (i < 0) return;
            if (w == 0) cfg.idx = i;
            else { cfg.photos.remove(i); if (cfg.idx >= cfg.photos.size()) cfg.idx = 0; fillThumbs(); }
            refresh();
        }).show();
    }

    void pick() {
        Intent i;
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            i = new Intent(MediaStore.ACTION_PICK_IMAGES);
            i.putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, Math.max(2, Math.min(30, MediaStore.getPickImagesMaxLimit())));
        } else {
            i = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*")
                    .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }
        try { startActivityForResult(i, PICK); }
        catch (Exception e) {
            startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("image/*").putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true), PICK);
        }
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != PICK || res != RESULT_OK || data == null) return;
        List<Uri> uris = new ArrayList<>();
        if (data.getClipData() != null) for (int i = 0; i < data.getClipData().getItemCount(); i++) uris.add(data.getClipData().getItemAt(i).getUri());
        else if (data.getData() != null) uris.add(data.getData());
        if (uris.isEmpty()) return;
        Toast.makeText(this, "Додаю фото…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            List<String> added = new ArrayList<>();
            for (Uri u : uris) {
                if (cfg.photos.size() + added.size() >= 30) break;
                try { added.add(Photos.importPhoto(this, u)); } catch (Exception ignored) {}
            }
            runOnUiThread(() -> {
                boolean first = cfg.photos.isEmpty();
                cfg.photos.addAll(added);
                if (first) cfg.idx = 0;
                if (added.size() < uris.size()) Toast.makeText(this, "Деякі фото не вдалося відкрити", Toast.LENGTH_SHORT).show();
                fillThumbs();
                refresh();
            });
        }).start();
    }

    void save() {
        AppWidgetManager m = AppWidgetManager.getInstance(this);
        if (type.equals("photo") && cfg.photos.isEmpty()) {
            Toast.makeText(this, "Спочатку додай хоча б одне фото", Toast.LENGTH_SHORT).show();
            return;
        }
        if (type.equals("photo") && cfg.idx >= cfg.photos.size()) cfg.idx = 0;
        if (type.equals("photo") && applyAll != null && applyAll.isChecked()) {
            int n = 0;
            for (int id : m.getAppWidgetIds(new ComponentName(this, PhotoWidget.class))) {
                if (id == widgetId) continue;
                Cfg o = Cfg.load(this, id, "photo");
                o.opacity = cfg.opacity; o.glass = cfg.glass;
                o.save(this, id);
                m.updateAppWidget(id, Render.build(this, o, id));
                n++;
            }
            if (n > 0) Toast.makeText(this, "Оновлено ще " + n + " фото-віджет(ів)", Toast.LENGTH_SHORT).show();
        }
        if (widgetId > 0) {
            cfg.save(this, widgetId);
            m.updateAppWidget(widgetId, Render.build(this, cfg, widgetId));
            setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId));
            Toast.makeText(this, "Збережено", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (!m.isRequestPinAppWidgetSupported()) {
            Toast.makeText(this, "Лаунчер не підтримує додавання з застосунку. Додай через: довге натискання → Віджети → SM Widget",
                    Toast.LENGTH_LONG).show();
            cfg.savePending(this);
            return;
        }
        cfg.savePending(this);
        Intent cb = new Intent(this, PinReceiver.class).putExtra("type", type);
        PendingIntent pi = PendingIntent.getBroadcast(this, type.hashCode(), cb,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        Bundle extras = new Bundle();
        extras.putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, Render.build(this, cfg, -1));
        m.requestPinAppWidget(new ComponentName(this, Render.provider(type)), extras, pi);
    }
}
