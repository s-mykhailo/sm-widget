package com.sm.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ImageDecoder;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.SizeF;
import android.widget.RemoteViews;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Імпорт фото, малювання форм/рамок/фільтрів і віддача картинки лаунчеру. */
public class Photos {
    public static final String[] SHAPES = {"Прямокутник", "Коло", "Арка", "Серце", "Печиво", "Квітка"};
    public static final String[] FILTERS = {"Без фільтра", "Ч/Б", "Сепія", "Тепле", "Холодне", "Вицвіле"};
    static final float[] RADIUS_DP = {0, 12, 22, 32, 9999};

    static File photosDir(Context c) { File d = new File(c.getFilesDir(), "photos"); d.mkdirs(); return d; }
    static File renderDir(Context c) { File d = new File(c.getFilesDir(), "render"); d.mkdirs(); return d; }

    /** Копіює фото з галереї у приватну папку (зменшене до 1600 px). Повертає ім'я файлу. */
    public static String importPhoto(Context c, Uri uri) throws Exception {
        ImageDecoder.Source s = ImageDecoder.createSource(c.getContentResolver(), uri);
        Bitmap b = ImageDecoder.decodeBitmap(s, (dec, info, src) -> {
            int w = info.getSize().getWidth(), h = info.getSize().getHeight();
            float k = Math.min(1f, 1600f / Math.max(w, h));
            dec.setTargetSize(Math.max(1, Math.round(w * k)), Math.max(1, Math.round(h * k)));
            dec.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
        });
        String name = UUID.randomUUID() + ".jpg";
        try (OutputStream o = new FileOutputStream(new File(photosDir(c), name))) {
            b.compress(Bitmap.CompressFormat.JPEG, 92, o);
        }
        b.recycle();
        return name;
    }

    /** Декодує фото так, щоб воно покривало w×h (center-crop). */
    public static Bitmap load(Context c, String name, int w, int h) {
        try {
            File f = new File(photosDir(c), name);
            if (!f.exists()) return null;
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(f), (dec, info, src) -> {
                int sw = info.getSize().getWidth(), sh = info.getSize().getHeight();
                float k = Math.min(1f, Math.max(w / (float) sw, h / (float) sh));
                dec.setTargetSize(Math.max(1, Math.round(sw * k)), Math.max(1, Math.round(sh * k)));
                dec.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            });
        } catch (Exception e) { return null; }
    }

    /** Розмір віджета в dp (портретна орієнтація). */
    public static float[] sizeDp(Context c, int id) {
        if (id <= 0) return new float[]{170, 170};
        Bundle o = AppWidgetManager.getInstance(c).getAppWidgetOptions(id);
        if (Build.VERSION.SDK_INT >= 31) {
            @SuppressWarnings("deprecation") ArrayList<SizeF> sizes = o.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES);
            if (sizes != null && !sizes.isEmpty()) {
                SizeF s = sizes.get(0);
                if (s.getWidth() > 0 && s.getHeight() > 0) return new float[]{s.getWidth(), s.getHeight()};
            }
        }
        int w = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH), h = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
        if (w <= 0 || h <= 0) return new float[]{170, 170};
        return new float[]{w, h};
    }

    /** RemoteViews для фото-віджета. id<=0: прев'ю з bitmap; інакше PNG-файл через ContentProvider. */
    public static RemoteViews remote(Context c, Cfg cfg, int id, int maxPx, Bitmap src) {
        RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.w_photo);
        float[] dp = sizeDp(c, id);
        float dens = c.getResources().getDisplayMetrics().density;
        float w = dp[0] * dens, h = dp[1] * dens;
        float k = Math.min(1f, maxPx / Math.max(w, h));
        int pw = Math.max(16, Math.round(w * k)), ph = Math.max(16, Math.round(h * k));
        boolean own = false;
        if (src == null && !cfg.photos.isEmpty()) {
            src = load(c, cfg.photos.get(Math.floorMod(cfg.idx, cfg.photos.size())), pw, ph);
            own = true;
        }
        Bitmap art = draw(c, cfg, src, pw, ph, dens * k);
        if (own && src != null) src.recycle();
        if (id <= 0) {
            rv.setImageViewBitmap(R.id.image, art);
        } else {
            String prefix = "w" + id + "_";
            String name = prefix + UUID.randomUUID() + ".png";
            File dir = renderDir(c);
            try (OutputStream o = new FileOutputStream(new File(dir, name))) {
                art.compress(Bitmap.CompressFormat.PNG, 100, o);
            } catch (Exception ignored) {}
            art.recycle();
            File[] old = dir.listFiles();
            if (old != null) for (File f : old) if (f.getName().startsWith(prefix) && !f.getName().equals(name)) f.delete();
            rv.setImageViewUri(R.id.image, Uri.parse("content://" + PhotoProvider.AUTH + "/render/" + name));
        }
        return rv;
    }

    static RectF square(RectF r) {
        float s = Math.min(r.width(), r.height());
        return new RectF(r.centerX() - s / 2, r.centerY() - s / 2, r.centerX() + s / 2, r.centerY() + s / 2);
    }

    static Path polar(RectF b, int waves, float amp) {
        Path p = new Path();
        float cx = b.centerX(), cy = b.centerY(), R = b.width() / 2;
        for (int i = 0; i <= 720; i++) {
            double t = Math.PI * 2 * i / 720;
            double r = R * (1 - amp + amp * Math.cos(waves * t));
            float x = (float) (cx + r * Math.cos(t - Math.PI / 2)), y = (float) (cy + r * Math.sin(t - Math.PI / 2));
            if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        p.close();
        return p;
    }

    /** Повертає контур і область, в яку кадрується фото. */
    static Path shape(Cfg cfg, RectF r, float dens, RectF[] cropOut) {
        Path p = new Path();
        switch (cfg.shape) {
            case 1: { RectF s = square(r); p.addOval(s, Path.Direction.CW); cropOut[0] = s; break; }
            case 2: {
                float rad = Math.min(r.width() / 2, r.height());
                p.moveTo(r.left, r.bottom);
                p.lineTo(r.left, r.top + rad);
                p.arcTo(new RectF(r.left, r.top, r.left + 2 * rad, r.top + 2 * rad), 180, 90, false);
                p.lineTo(r.right - rad, r.top);
                p.arcTo(new RectF(r.right - 2 * rad, r.top, r.right, r.top + 2 * rad), 270, 90, false);
                p.lineTo(r.right, r.bottom);
                p.close();
                cropOut[0] = r; break;
            }
            case 3: {
                RectF s = square(r); float x = s.left, y = s.top, n = s.width();
                p.moveTo(x + .5f * n, y + .93f * n);
                p.cubicTo(x + .2f * n, y + .73f * n, x, y + .53f * n, x, y + .32f * n);
                p.cubicTo(x, y + .14f * n, x + .14f * n, y + .04f * n, x + .28f * n, y + .04f * n);
                p.cubicTo(x + .38f * n, y + .04f * n, x + .46f * n, y + .1f * n, x + .5f * n, y + .2f * n);
                p.cubicTo(x + .54f * n, y + .1f * n, x + .62f * n, y + .04f * n, x + .72f * n, y + .04f * n);
                p.cubicTo(x + .86f * n, y + .04f * n, x + n, y + .14f * n, x + n, y + .32f * n);
                p.cubicTo(x + n, y + .53f * n, x + .8f * n, y + .73f * n, x + .5f * n, y + .93f * n);
                p.close();
                cropOut[0] = s; break;
            }
            case 4: { RectF s = square(r); p = polar(s, 9, 0.055f); cropOut[0] = s; break; }
            case 5: { RectF s = square(r); p = polar(s, 6, 0.13f); cropOut[0] = s; break; }
            default: {
                float rad = Math.min(RADIUS_DP[Math.max(0, Math.min(4, cfg.radius))] * dens, Math.min(r.width(), r.height()) / 2);
                p.addRoundRect(r, rad, rad, Path.Direction.CW);
                cropOut[0] = r;
            }
        }
        return p;
    }

    static ColorMatrix filter(int f) {
        ColorMatrix m = new ColorMatrix();
        switch (f) {
            case 1: m.setSaturation(0); break;
            case 2: { m.setSaturation(0); ColorMatrix t = new ColorMatrix(); t.setScale(1.07f, 0.95f, 0.8f, 1); m.postConcat(t); break; }
            case 3: m.setScale(1.08f, 1.0f, 0.86f, 1); break;
            case 4: m.setScale(0.9f, 1.0f, 1.12f, 1); break;
            case 5: {
                m.setSaturation(0.55f);
                ColorMatrix t = new ColorMatrix(new float[]{0.85f, 0, 0, 0, 30, 0, 0.85f, 0, 0, 30, 0, 0, 0.85f, 0, 34, 0, 0, 0, 1, 0});
                m.postConcat(t); break;
            }
        }
        return m;
    }

    static final float[] BORDER_DP = {0, 3, 7};

    public static Bitmap draw(Context c, Cfg cfg, Bitmap src, int w, int h, float dens) {
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(out);
        float bw = BORDER_DP[Math.max(0, Math.min(2, cfg.border))] * dens;
        RectF r = new RectF(bw / 2 + 1, bw / 2 + 1, w - bw / 2 - 1, h - bw / 2 - 1);
        RectF[] crop = new RectF[1];
        Path path = shape(cfg, r, dens, crop);
        RectF cb = crop[0];
        int op = Math.max(5, Math.min(100, cfg.opacity));
        if (op < 100) cv.saveLayerAlpha(null, Math.round(op * 2.55f));

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        if (src != null) {
            BitmapShader sh = new BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            float k = Math.max(cb.width() / src.getWidth(), cb.height() / src.getHeight());
            Matrix m = new Matrix();
            m.setScale(k, k);
            m.postTranslate(cb.centerX() - src.getWidth() * k / 2, cb.centerY() - src.getHeight() * k / 2);
            sh.setLocalMatrix(m);
            p.setShader(sh);
            if (cfg.filter != 0) p.setColorFilter(new ColorMatrixColorFilter(filter(cfg.filter)));
        } else {
            p.setShader(new LinearGradient(cb.left, cb.top, cb.right, cb.bottom, 0xFF8AB4F8, 0xFFC58AF9, Shader.TileMode.CLAMP));
        }
        cv.drawPath(path, p);

        String cap = src == null ? "Додай фото" : cfg.text1.trim();
        if (!cap.isEmpty()) {
            float scale = Render.SCALE[Math.max(0, Math.min(3, cfg.size))];
            TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            tp.setColor(0xFFFFFFFF);
            tp.setTextSize(15 * scale * dens);
            tp.setTypeface(Typeface.create(Cfg.FONT_FAMILIES[cfg.fontIndex()], Typeface.NORMAL));
            tp.setShadowLayer(3 * dens, 0, dens, 0x99000000);
            boolean round = cfg.shape != 0 && cfg.shape != 2;
            int tw = Math.max(10, Math.round(cb.width() * (round ? 0.62f : 0.86f)));
            StaticLayout sl = StaticLayout.Builder.obtain(cap, 0, cap.length(), tp, tw)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER).setMaxLines(3).setEllipsize(android.text.TextUtils.TruncateAt.END).build();
            float th = sl.getHeight();
            float bottom = src == null ? cb.centerY() + th / 2 : cb.bottom - cb.height() * (round ? 0.2f : 0.07f);
            if (src != null) {
                Paint scrim = new Paint(Paint.ANTI_ALIAS_FLAG);
                float top = Math.max(cb.top, bottom - th - cb.height() * 0.25f);
                scrim.setShader(new LinearGradient(0, top, 0, cb.bottom, 0x00000000, 0x8C000000, Shader.TileMode.CLAMP));
                cv.save();
                cv.clipRect(cb.left - bw, top, cb.right + bw, cb.bottom + bw);
                cv.drawPath(path, scrim);
                cv.restore();
            }
            cv.save();
            cv.translate(cb.centerX() - tw / 2f, bottom - th);
            sl.draw(cv);
            cv.restore();
        }

        if (bw > 0) {
            Paint b = new Paint(Paint.ANTI_ALIAS_FLAG);
            b.setStyle(Paint.Style.STROKE);
            b.setStrokeWidth(bw);
            b.setStrokeJoin(Paint.Join.ROUND);
            b.setColor(cfg.fgColor(c));
            cv.drawPath(path, b);
        }

        if (cfg.glass > 0) {
            boolean strong = cfg.glass == 2;
            Paint g = new Paint(Paint.ANTI_ALIAS_FLAG);
            // молочна вуаль
            g.setColor(strong ? 0x38FFFFFF : 0x1FFFFFFF);
            cv.drawPath(path, g);
            // відблиск зверху-зліва
            g.setColor(0xFFFFFFFF);
            g.setShader(new LinearGradient(cb.left, cb.top, cb.centerX(), cb.centerY(),
                    strong ? 0x66FFFFFF : 0x40FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
            cv.drawPath(path, g);
            // тонкий світлий край
            if (bw == 0) {
                Paint e = new Paint(Paint.ANTI_ALIAS_FLAG);
                e.setStyle(Paint.Style.STROKE);
                e.setStrokeWidth(Math.max(1f, 1.2f * dens));
                e.setColor(strong ? 0x99FFFFFF : 0x66FFFFFF);
                cv.drawPath(path, e);
            }
        }
        if (op < 100) cv.restore();
        return out;
    }

    /** Видаляє фото, які не використовує жоден віджет. */
    public static void gc(Context c) {
        Set<String> used = new HashSet<>();
        for (Map.Entry<String, ?> e : Cfg.prefs(c).getAll().entrySet()) {
            if (!(e.getValue() instanceof String)) continue;
            Cfg x = Cfg.fromJson((String) e.getValue(), "photo");
            used.addAll(x.photos);
        }
        File[] fs = photosDir(c).listFiles();
        if (fs != null) for (File f : fs) if (!used.contains(f.getName())) f.delete();
    }

    public static void deleteRenders(Context c, int id) {
        File[] fs = renderDir(c).listFiles();
        if (fs != null) for (File f : fs) if (f.getName().startsWith("w" + id + "_")) f.delete();
    }
}
