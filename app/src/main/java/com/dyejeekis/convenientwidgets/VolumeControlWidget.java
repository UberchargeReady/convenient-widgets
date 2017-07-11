package com.dyejeekis.convenientwidgets;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * Created by George on 7/10/2017.
 */

public class VolumeControlWidget extends AppWidgetProvider {

    public static final String TAG = "VolumeControl";

    public static final String EXTRA_INCREASE_VOLUME = "INCREASE_VOLUME";
    public static final String EXTRA_DECREASE_VOLUME = "DECREASE_VOLUME";
    public static final String EXTRA_TOGGLE_CURRENT_STREAM = "VOLUME_DEFAULT";

    private static final int[] AUDIO_STREAMS = {AudioManager.STREAM_MUSIC, AudioManager.STREAM_RING, AudioManager.STREAM_ALARM};

    private static boolean initialized = false;
    private static AudioManager audioManager;
    private static int streamIndex;
    private static int[] streamVolumes = new int[3];
    private static ContentObserver observer;

    @Override
    public void onReceive(Context context, Intent intent) {
        //Log.d(TAG, "onReceive");

        // initialize here
        if(!initialized) {
            initialized = true;
            audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            registerContentObserver(context);
        }

        if(intent.getBooleanExtra(EXTRA_INCREASE_VOLUME, false)) {
            if(!checkPermission(context)) {
                return;
            }
            increaseVolume();
        }
        else if(intent.getBooleanExtra(EXTRA_DECREASE_VOLUME, false)) {
            if(!checkPermission(context)) {
                return;
            }
            decreaseVolume();
        }
        else if(intent.getBooleanExtra(EXTRA_TOGGLE_CURRENT_STREAM, false)) {
            toggleCurrentStream();
        }
        super.onReceive(context, intent);
    }

    private void increaseVolume() {
        audioManager.adjustStreamVolume(AUDIO_STREAMS[streamIndex], AudioManager.ADJUST_RAISE, 0);
    }

    private void decreaseVolume() {
        audioManager.adjustStreamVolume(AUDIO_STREAMS[streamIndex], AudioManager.ADJUST_LOWER, 0);
    }

    private void toggleCurrentStream() {
        streamIndex++;
        if(streamIndex>AUDIO_STREAMS.length-1) {
            streamIndex = 0;
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        //Log.d(TAG, "onUpdate");
        ComponentName provider = new ComponentName(context, VolumeControlWidget.class);
        appWidgetManager.updateAppWidget(provider, buildUpdate(context, appWidgetIds));
    }

    private RemoteViews buildUpdate(Context context, int[] appWidgetIds) {
        RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.widget_volume_control);

        updateViews.setOnClickPendingIntent(R.id.imageView_decrease_volume,
                getPendingIntent(context, appWidgetIds, EXTRA_DECREASE_VOLUME));
        updateViews.setOnClickPendingIntent(R.id.imageView_increase_volume,
                getPendingIntent(context, appWidgetIds, EXTRA_INCREASE_VOLUME));
        updateViews.setOnClickPendingIntent(R.id.imageView_current_stream,
                getPendingIntent(context, appWidgetIds, EXTRA_TOGGLE_CURRENT_STREAM));

        updateStreamVolumes();
        String text = "";
        int resource = -1;
        switch (AUDIO_STREAMS[streamIndex]) {
            case AudioManager.STREAM_RING:
                text = "Ringer volume";
                resource = R.drawable.ic_notifications_white_48dp;
                break;
            case AudioManager.STREAM_MUSIC:
                text = "Media volume";
                resource = R.drawable.ic_music_note_white_48dp;
                break;
            case AudioManager.STREAM_ALARM:
                text = "Alarm volume";
                resource = R.drawable.ic_alarm_white_48dp;
                break;
        }
        updateViews.setTextViewText(R.id.textView_volume_info, text + " " + streamVolumes[streamIndex]);
        updateViews.setImageViewResource(R.id.imageView_current_stream, resource);
        updateViews.setImageViewResource(R.id.imageView_decrease_volume, R.drawable.ic_remove_white_48dp);
        updateViews.setImageViewResource(R.id.imageView_increase_volume, R.drawable.ic_add_white_48dp);

        return updateViews;
    }

    private PendingIntent getPendingIntent(Context context, int[] appWidgetIds, String extra) {
        Intent intent = new Intent(context, VolumeControlWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        intent.putExtra(extra, true);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, extra.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }

    private void updateWidget(Context context) {
        Intent intent = new Intent(context, VolumeControlWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, VolumeControlWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.sendBroadcast(intent);
    }

    private void updateStreamVolumes() {
        for(int i=0; i<AUDIO_STREAMS.length;i++) {
            streamVolumes[i] = audioManager.getStreamVolume(AUDIO_STREAMS[i]);
        }
    }

    @Override
    public void onEnabled(Context context) {
        //Log.d(TAG, "onEnabled");
        super.onEnabled(context);
        streamIndex = 0;
        //registerContentObserver(context);
    }

    @Override
    public void onDisabled(Context context) {
        //Log.d(TAG, "onDisabled");
        unregisterContentObserver(context);
        super.onDisabled(context);
    }

    private void registerContentObserver(final Context context) {
        observer = new ContentObserver(new Handler()) {
            @Override
            public boolean deliverSelfNotifications() {
                return true;
            }

            @Override
            public void onChange(boolean selfChange) {
                //Log.d(TAG, "onChange");
                super.onChange(selfChange);
                if (streamVolumes[0] != audioManager.getStreamVolume(AUDIO_STREAMS[0])
                        || streamVolumes[1] != audioManager.getStreamVolume(AUDIO_STREAMS[1])
                        || streamVolumes[2] != audioManager.getStreamVolume(AUDIO_STREAMS[2])) {
                    updateWidget(context);
                }
            }
        };
        Uri uri = Settings.System.CONTENT_URI;
        context.getContentResolver().registerContentObserver(uri, true, observer);
    }

    private void unregisterContentObserver(Context context) {
        if(observer!=null) {
            context.getContentResolver().unregisterContentObserver(observer);
        }
    }

    /**
     *
     * @param context
     * @return true if permission is already granted - false if not
     */
    private boolean checkPermission(Context context) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && AUDIO_STREAMS[streamIndex] == AudioManager.STREAM_RING) {
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if(notificationManager.isNotificationPolicyAccessGranted()) {
                return true;
            }
            else {
                Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                context.startActivity(intent);
                return false;
            }
        }
        return true;
    }

}
