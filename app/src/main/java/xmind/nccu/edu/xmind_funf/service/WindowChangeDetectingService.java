package xmind.nccu.edu.xmind_funf.Service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;

import xmind.nccu.edu.xmind_funf.Util.FunfDataBaseHelper;

/**
 * Created by sid.ku on 8/20/15.
 */
public class WindowChangeDetectingService extends AccessibilityService {

    private String previousActivity = "";

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        //Configure these here for compatibility with API 13 and below.
        AccessibilityServiceInfo config = new AccessibilityServiceInfo();
        config.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        config.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;

        if (Build.VERSION.SDK_INT >= 16)
            //Just in case this helps
            config.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;


        //Starting service from here...
        Intent intent_initService = new Intent(this, XmindService.class);
        intent_initService.setAction(XmindService.FIRST_TIME_START_SERVICE);
        startService(intent_initService);

        setServiceInfo(config);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            ComponentName componentName = new ComponentName(
                    event.getPackageName().toString(),
                    event.getClassName().toString()
            );

            ActivityInfo activityInfo = tryGetActivity(componentName);
            boolean isActivity = activityInfo != null;
            if (isActivity) {//get activity change here.
                if (!componentName.flattenToShortString().equals(previousActivity)) {
//                    Log.i("ssku", componentName.flattenToShortString());
                    FunfDataBaseHelper FDB_Helper = new FunfDataBaseHelper(this, FunfDataBaseHelper.XMIND_FUNF_DATABASE_NAME);
                    FDB_Helper.addCurrentForegroundAppRecord(FunfDataBaseHelper.CURRENT_FOREGROUND_APP, String.valueOf(System.currentTimeMillis()), componentName.flattenToShortString());
                    FDB_Helper.close();
                } /*else
                    Log.v("ssku", "Same activity, wouldn't record it.");*/
                previousActivity = componentName.flattenToShortString();
            }
        }
    }

    private ActivityInfo tryGetActivity(ComponentName componentName) {
        try {
            return getPackageManager().getActivityInfo(componentName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @Override
    public void onInterrupt() {
    }
}
