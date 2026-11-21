package com.sm.widget;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Отримує id щойно закріпленого віджета і прив'язує до нього налаштування з редактора. */
public class PinReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        int id = i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        String type = i.getStringExtra("type");
        if (id <= 0 || type == null) return;
        Cfg cfg = Cfg.fromJson(Cfg.pending(c, type), type);
        cfg.save(c, id);
        AppWidgetManager.getInstance(c).updateAppWidget(id, Render.build(c, cfg, id));
    }
}
