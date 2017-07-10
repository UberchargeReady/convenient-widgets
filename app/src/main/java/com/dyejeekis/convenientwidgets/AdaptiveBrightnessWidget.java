package com.dyejeekis.convenientwidgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.widget.RemoteViews;

/**
 * Created by George on 7/6/2017.
 */

public class AdaptiveBrightnessWidget extends AppWidgetProvider {

    public static final String TAG = "AdaptiveBrightnessWidget";

    public static final String EXTRA_TOGGLE_BRIGHTNESS = "TOGGLE_BRIGHTNESS";

    private static ContentObserver observer;

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getBooleanExtra(EXTRA_TOGGLE_BRIGHTNESS, false)) {
            toggleBrightness(context);
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        ComponentName me = new ComponentName(context, AdaptiveBrightnessWidget.class);
        appWidgetManager.updateAppWidget(me, buildUpdate(context, appWidgetIds));
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        registerSettingObserver(context);
    }

    @Override
    public void onDisabled(Context context) {
        unregisterSettingObserver(context);
        super.onDisabled(context);
    }

    private void registerSettingObserver(final Context context) {
        Uri setting = Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE);
        observer = new ContentObserver(new Handler()) {
            @Override
            public boolean deliverSelfNotifications() {
                return true;
            }

            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                updateWidget(context);
            }
        };
        context.getContentResolver().registerContentObserver(setting, false, observer);
    }

    private void updateWidget(Context context) {
        Intent intent = new Intent(context, AdaptiveBrightnessWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, AdaptiveBrightnessWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.sendBroadcast(intent);
    }

    private void unregisterSettingObserver(Context context) {
        if(observer!=null) {
            context.getContentResolver().unregisterContentObserver(observer);
        }
    }

    private RemoteViews buildUpdate(Context context, int[] appWidgetIds) {
        RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.widget_adaptive_brightness);

        updateViews.setOnClickPendingIntent(R.id.imageView_brightness_mode, getPendingIntent(context, appWidgetIds));

        int currentMode = getCurrentBrightnessMode(context);
        int resource = -1;
        switch (currentMode) {
            case Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC:
                resource = R.drawable.ic_brightness_auto_white_48dp;
                break;
            case Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL:
                resource = R.drawable.ic_brightness_7_white_48dp;
                break;
        }
        if(resource!=-1) {
            updateViews.setImageViewResource(R.id.imageView_brightness_mode, resource);
        }

        return updateViews;
    }

    private PendingIntent getPendingIntent(Context context, int[] appWidgetIds) {
        Intent intent = new Intent(context, AdaptiveBrightnessWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        intent.putExtra(EXTRA_TOGGLE_BRIGHTNESS, true);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, EXTRA_TOGGLE_BRIGHTNESS.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }

    private int getCurrentBrightnessMode(Context context) {
        try {
            return Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private void toggleBrightness(Context context) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + context.getPackageName()));
            context.startActivity(intent);
            return;
        }
        int currentMode = getCurrentBrightnessMode(context);
        currentMode = currentMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC ?
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL : Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
        Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, currentMode);
    }

}
