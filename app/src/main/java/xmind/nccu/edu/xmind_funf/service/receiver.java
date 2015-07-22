package xmind.nccu.edu.xmind_funf.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * Created by sid.ku on 6/22/15.
 */
public class receiver extends BroadcastReceiver {
    private static final String TAG = "ssku";//receiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && intent.getAction() != null) {
            Log.v(TAG, "@Receiver, get action : " + intent.getAction().toString());

            //testing...
            WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifi.isWifiEnabled()) {
                Log.w(TAG, "Wifi connected...");
            } else {
                Log.w(TAG, "Wifi disconnected...");
            }


            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                Log.v(TAG, "@Receiver, After reboot, starting service.");
                Intent intent_initService = new Intent(context, xmind_service.class);
                intent_initService.setAction(xmind_service.FIRST_TIME_START_SERVICE);
                context.startService(intent_initService);
            } else if (intent.getAction().equals(xmind_service.CHECK_POINT)) {
                Log.v(TAG, "@Receiver, CHECK_POINT.");
                Intent intent_checkpoint = new Intent(context, xmind_service.class);
                intent_checkpoint.setAction(xmind_service.CHECK_POINT);
                context.startService(intent_checkpoint);
            } else if (intent.getAction().equals("com.android.camera.NEW_PICTURE") || intent.getAction().equals("android.hardware.action.NEW_PICTURE")) {
                Log.v(TAG, "Just taking a picture, get action : " + intent.getAction().toString());
                Intent intent_takepicture = new Intent(context, xmind_service.class);
                intent_takepicture.setAction(xmind_service.TAKE_PICTURE);
                context.startService(intent_takepicture);
            } else {
                Log.w(TAG, "Unknow action, do nothing.");
            }
        }
    }
}
