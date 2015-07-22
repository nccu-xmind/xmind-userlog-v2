package xmind.nccu.edu.xmind_funf;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

/**
 * Created by sid.ku on 6/24/15.
 */
public class GetCurrentRunningApp {

    private String currentAppName = "";

    public GetCurrentRunningApp(Context mContext) {
        ActivityManager mActivityManager =(ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
        String mPackageName = "";
        if(Build.VERSION.SDK_INT > 20){
            mPackageName = mActivityManager.getRunningAppProcesses().get(0).processName;
        }
        else{
            mPackageName = mActivityManager.getRunningTasks(1).get(0).topActivity.getPackageName();
        }
        Log.v("ssku", "====currentTaksPackageName : " + mPackageName);
        currentAppName = mPackageName;
    }

    public String getCurrentAppName(){
        return currentAppName;
    }
}
