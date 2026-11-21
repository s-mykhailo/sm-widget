package com.sm.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;

public abstract class BaseWidget extends AppWidgetProvider {
    abstract String type();

    @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) m.updateAppWidget(id, Render.build(c, Cfg.load(c, id, type()), id));
    }

    @Override public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle o) {
        m.updateAppWidget(id, Render.build(c, Cfg.load(c, id, type()), id));
    }

    @Override public void onDeleted(Context c, int[] ids) {
        for (int id : ids) Cfg.delete(c, id);
    }
}
