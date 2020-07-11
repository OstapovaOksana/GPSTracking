package com.gpstracking;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MyReceiver extends BroadcastReceiver {

    //MyService serv = new MyService();
    @Override
    public void onReceive(Context context, Intent intent) {
            Log.i("RECEIVER", "ACTION_SCREEN_OFF");
            Intent a = new Intent(context,MyService.class);
            context.startService(a);

    }
}
