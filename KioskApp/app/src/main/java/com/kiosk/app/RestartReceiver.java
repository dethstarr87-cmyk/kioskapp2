package com.kiosk.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Receives a restart broadcast (sent by the app itself or a watchdog)
 * and relaunches KioskActivity. This provides basic crash recovery.
 */
public class RestartReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent kioskIntent = new Intent(context, KioskActivity.class);
        kioskIntent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK |
            Intent.FLAG_ACTIVITY_CLEAR_TOP
        );
        context.startActivity(kioskIntent);
    }
}
