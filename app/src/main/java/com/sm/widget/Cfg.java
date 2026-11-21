package com.sm.widget;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

/** Налаштування одного віджета. Зберігаються локально в SharedPreferences. */
public class Cfg {
    public static final int DYN1 = 1, DYN2 = 2; // кольори Material You (не реальні ARGB)
    public static final String[] TYPES = {"photo", "clock", "text", "battery", "progress", "countdown"};
    public static final String[] FONT_KEYS = {"sans", "light", "thin", "black", "cond", "serif", "mono", "cursive"};
    public static final String[] FONT_FAMILIES = {"sans-serif", "sans-serif-light", "sans-serif-thin", "sans-serif-black",
            "sans-serif-condensed", "serif", "monospace", "cursive"};
    public static final String[] FONT_NAMES = {"Звичайний", "Тонкий", "Ультратонкий", "Жирний", "Вузький", "З засічками", "Моно", "Рукописний"};

    public String type = "clock";
    public String font = "sans";
    public int bg = 0xFF202124, fg = 0xFFFFFFFF;
    public int alpha = 100, radius = 2, size = 1, align = 1, mode = 0;
    public String text1 = "", text2 = "";
    public long date = LocalDate.of(LocalDate.now().getYear() + 1, 1, 1).toEpochDay();
    public boolean yearly = true;
    public List<String> photos = new ArrayList<>();
    public int shape = 0, border = 0, filter = 0, tap = 0, every = 0, idx = 0;
    public long lastDay = 0;
    public int opacity = 100, glass = 0;

    static boolean dynOk() { return Build.VERSION.SDK_INT >= 31; }

    public static Cfg def(String type) {
        Cfg c = new Cfg();
        c.type = type;
        boolean d = dynOk();
        switch (type) {
            case "photo": c.radius = 3; c.fg = 0xFFFFFFFF; break;
            case "clock": c.font = "light"; c.alpha = 0; c.fg = 0xFFFFFFFF; break;
            case "text": c.text1 = "Роби. Не чекай."; c.bg = d ? DYN1 : 0xFFF1F3F4; c.fg = d ? DYN1 : 0xFF202124; c.radius = 3; break;
            case "battery": c.bg = d ? DYN2 : 0xFF202124; c.fg = d ? DYN2 : 0xFF8AB4F8; c.radius = 4; break;
            case "progress": c.mode = 3; c.alpha = 90; c.fg = 0xFF8AB4F8; break;
            case "countdown": c.text1 = "Нового року"; c.bg = d ? DYN1 : 0xFFFDD663; c.fg = d ? DYN1 : 0xFF202124; c.radius = 3; break;
        }
        return c;
    }

    public int bgColor(Context ctx) {
        if (bg == DYN1) return dynOk() ? ctx.getColor(android.R.color.system_accent1_100) : 0xFFD2E3FC;
        if (bg == DYN2) return dynOk() ? ctx.getColor(android.R.color.system_neutral1_900) : 0xFF202124;
        return bg | 0xFF000000;
    }

    public int fgColor(Context ctx) {
        if (fg == DYN1) return dynOk() ? ctx.getColor(android.R.color.system_accent1_900) : 0xFF0B2B5C;
        if (fg == DYN2) return dynOk() ? ctx.getColor(android.R.color.system_accent1_100) : 0xFFD2E3FC;
        return fg | 0xFF000000;
    }

    public int fontIndex() {
        for (int i = 0; i < FONT_KEYS.length; i++) if (FONT_KEYS[i].equals(font)) return i;
        return 0;
    }

    public String toJson() {
        try {
            JSONObject o = new JSONObject();
            o.put("type", type); o.put("font", font); o.put("bg", bg); o.put("fg", fg);
            o.put("alpha", alpha); o.put("radius", radius); o.put("size", size); o.put("align", align);
            o.put("mode", mode); o.put("text1", text1); o.put("text2", text2); o.put("date", date); o.put("yearly", yearly);
            o.put("photos", new JSONArray(photos)); o.put("shape", shape); o.put("border", border); o.put("filter", filter);
            o.put("tap", tap); o.put("every", every); o.put("idx", idx); o.put("lastDay", lastDay); o.put("opacity", opacity); o.put("glass", glass);
            return o.toString();
        } catch (Exception e) { return "{}"; }
    }

    public static Cfg fromJson(String s, String type) {
        Cfg c = def(type);
        if (s == null) return c;
        try {
            JSONObject o = new JSONObject(s);
            c.font = o.optString("font", c.font); c.bg = o.optInt("bg", c.bg); c.fg = o.optInt("fg", c.fg);
            c.alpha = o.optInt("alpha", c.alpha); c.radius = o.optInt("radius", c.radius); c.size = o.optInt("size", c.size);
            c.align = o.optInt("align", c.align); c.mode = o.optInt("mode", c.mode);
            c.text1 = o.optString("text1", c.text1); c.text2 = o.optString("text2", c.text2);
            c.date = o.optLong("date", c.date); c.yearly = o.optBoolean("yearly", c.yearly);
            JSONArray a = o.optJSONArray("photos");
            if (a != null) for (int i = 0; i < a.length(); i++) c.photos.add(a.getString(i));
            c.shape = o.optInt("shape", c.shape); c.border = o.optInt("border", c.border); c.filter = o.optInt("filter", c.filter);
            c.tap = o.optInt("tap", c.tap); c.every = o.optInt("every", c.every); c.idx = o.optInt("idx", c.idx);
            c.lastDay = o.optLong("lastDay", c.lastDay);
            c.opacity = o.optInt("opacity", c.opacity); c.glass = o.optInt("glass", c.glass);
        } catch (Exception ignored) {}
        c.type = type;
        return c;
    }

    public Cfg copy() { return fromJson(toJson(), type); }

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("widgets", Context.MODE_PRIVATE); }

    public static Cfg load(Context c, int id, String type) {
        SharedPreferences p = prefs(c);
        String s = p.getString("w_" + id, null);
        if (s == null) s = p.getString("p_" + type, null);
        return fromJson(s, type);
    }

    public static boolean has(Context c, int id) { return prefs(c).contains("w_" + id); }
    public void save(Context c, int id) { prefs(c).edit().putString("w_" + id, toJson()).apply(); }
    public void savePending(Context c) { prefs(c).edit().putString("p_" + type, toJson()).apply(); }
    public static String pending(Context c, String type) { return prefs(c).getString("p_" + type, null); }
    public static void delete(Context c, int id) { prefs(c).edit().remove("w_" + id).apply(); }
}
