package xmind.nccu.edu.xmind_funf.Util;

import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Build;

import java.util.Calendar;
import java.util.List;

/**
 * Created by sid.ku on 6/24/15.
 */
public class GetCurrentRunningApp {

    public static final String USAGE_STATS_SERVICE = "usagestats";

    private String currentAppName = "";
    private ActivityManager mActivityManager;

//    public GetCurrentRunningApp(Context mContext) {
//        ActivityManager mActivityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
//        String mPackageName = "";
//        if (Build.VERSION.SDK_INT > 21) {
//            ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
//            List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
//            if (!tasks.isEmpty()) {
//                ComponentName topActivity = tasks.get(0).topActivity;
////                Log.v("ssku", "Current packagename : " + topActivity.getPackageName());
//                mPackageName = topActivity.getPackageName();
//            } else
//                mPackageName = "Unknow app";
//            Log.w("ssku", "(1)Current aap : " + mPackageName);
//        } else {
//            try {
//                mPackageName = mActivityManager.getRunningTasks(1).get(0).topActivity.getPackageName();
//            } catch (Exception e) {
//                mPackageName = "Unknow app";
//            }
//            Log.w("ssku", "(2)Current aap : " + mPackageName);
//        }
//        currentAppName = mPackageName;
//    }

    public GetCurrentRunningApp(Context mContext) {
        /*ActivityManager*/
        mActivityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        String mPackageName = "";
        if (Build.VERSION.SDK_INT > 20) {
            mPackageName = mActivityManager.getRunningAppProcesses().get(0).processName;
//            Log.w("ssku", "(1)Current aap : " + mPackageName);
        } else {
            mPackageName = mActivityManager.getRunningTasks(1).get(0).topActivity.getPackageName();
//            Log.w("ssku", "(2)Current aap : " + mPackageName);
        }
//        getLollipopApp(mContext);
        printForegroundTask(mContext);
        currentAppName = mPackageName;
    }

    public String getCurrentAppName() {
        return currentAppName;
    }

    private void printForegroundTask(Context mContext) {
//        String currentApp = "NULL";
//        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
//            UsageStatsManager usm = (UsageStatsManager) mContext.getSystemService(Context.USAGE_STATS_SERVICE);
//            long time = System.currentTimeMillis();
//            List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,  time - 1000*1000, time);
//
//            if(appList != null){
//                Log.v("ssku", "List size : " + appList.size());
//            }
//
//            if (appList != null && appList.size() > 0) {
//                SortedMap<Long, UsageStats> mySortedMap = new TreeMap<Long, UsageStats>();
//                for (UsageStats usageStats : appList) {
//                    mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
//                }
//                if (mySortedMap != null && !mySortedMap.isEmpty()) {
//                    currentApp = mySortedMap.get(mySortedMap.lastKey()).getPackageName();
//                }
//            }
//        } else {
//            ActivityManager am = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
//            List<ActivityManager.RunningAppProcessInfo> tasks = am.getRunningAppProcesses();
//            currentApp = tasks.get(0).processName;
//        }
//
//        Log.e("ssku", "Current App in foreground is: " + currentApp);
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            final UsageStatsManager usageStatsManager = (UsageStatsManager) mContext.getSystemService("usagestats");// Context.USAGE_STATS_SERVICE);
            final int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            final List<UsageStats> queryUsageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_YEARLY, currentYear - 2, currentYear);

        }
    }
}
