package com.sm.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import java.time.LocalDate;

public class PhotoWidget extends BaseWidget {
    public static final String NEXT = "com.sm.widget.NEXT";

    @Override String type() { return "photo"; }

    @Override public void onReceive(Context c, Intent i) {
        if (NEXT.equals(i.getAction())) {
            int id = i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if (id <= 0) return;
            Cfg cfg = Cfg.load(c, id, "photo");
            if (!cfg.photos.isEmpty()) { cfg.idx = (cfg.idx + 1) % cfg.photos.size(); cfg.save(c, id); }
            AppWidgetManager.getInstance(c).updateAppWidget(id, Render.build(c, cfg, id));
            return;
        }
        super.onReceive(c, i);
    }

    @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) {
            Cfg cfg = Cfg.load(c, id, "photo");
            boolean ch = false;
            if (cfg.photos.size() > 1) {
                if (cfg.every == 1) { cfg.idx++; ch = true; }
                else if (cfg.every == 2) {
                    long today = LocalDate.now().toEpochDay();
                    if (cfg.lastDay != today) { if (cfg.lastDay != 0) cfg.idx++; cfg.lastDay = today; ch = true; }
                }
                cfg.idx = Math.floorMod(cfg.idx, cfg.photos.size());
            }
            if (ch && Cfg.has(c, id)) cfg.save(c, id);
            m.updateAppWidget(id, Render.build(c, cfg, id));
        }
    }

    @Override public void onDeleted(Context c, int[] ids) {
        super.onDeleted(c, ids);
        for (int id : ids) Photos.deleteRenders(c, id);
        Photos.gc(c);
    }
}
