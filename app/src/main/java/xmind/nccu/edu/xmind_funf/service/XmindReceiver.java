package xmind.nccu.edu.xmind_funf.Service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by sid.ku on 6/22/15.
 */
public class XmindReceiver extends BroadcastReceiver {
    private static final String TAG = XmindReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && intent.getAction() != null) {
//            Log.v(TAG, "@Receiver, get action : " + intent.getAction().toString());
            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                Intent intent_initService = new Intent(context, XmindService.class);
                intent_initService.setAction(XmindService.FIRST_TIME_START_SERVICE);
                context.startService(intent_initService);
            } else if (intent.getAction().equals(XmindService.CHECK_POINT)) {
                Intent intent_checkpoint = new Intent(context, XmindService.class);
                intent_checkpoint.setAction(XmindService.CHECK_POINT);
                context.startService(intent_checkpoint);
            } else if (intent.getAction().equals(XmindService.SERVICE_PROBE)) {
                Intent intent_serviceprobe = new Intent(context, XmindService.class);
                intent_serviceprobe.setAction(XmindService.SERVICE_PROBE);
                context.startService(intent_serviceprobe);
            } else if (intent.getAction().equals(XmindService.UPLOADING_REMINDER)) {
                Intent intent_Upload_Reminder = new Intent(context, XmindService.class);
                intent_Upload_Reminder.setAction(XmindService.UPLOADING_REMINDER);
                context.startService(intent_Upload_Reminder);
            } else if (intent.getAction().equals(XmindService.CALLLOG_REMINDER)) {
                Intent intent_CallLog_Reminder = new Intent(context, XmindService.class);
                intent_CallLog_Reminder.setAction(XmindService.CALLLOG_REMINDER);
                context.startService(intent_CallLog_Reminder);
            } else if (intent.getAction().equals("android.hardware.action.NEW_PICTURE")) {//add 'com.android.camera.NEW_PICTURE'(Removed) if we need supporting android 3.x or previous version.
                //Sometimes, Receiver get twice new_Picture Action at same times.
                Intent intent_takepicture = new Intent(context, XmindService.class);
                intent_takepicture.setAction(XmindService.TAKE_PICTURE);
                context.startService(intent_takepicture);
            } else {
                Log.w(TAG, "Unknow action, do nothing.");
            }
        } else
            Log.w(TAG, "Intent is null or empty action.");
    }
}
