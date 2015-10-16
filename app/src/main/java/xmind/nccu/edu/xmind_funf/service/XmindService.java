package xmind.nccu.edu.xmind_funf.Service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.FileObserver;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.pipeline.BasicPipeline;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.builtin.BatteryProbe;
import edu.mit.media.funf.probe.builtin.BluetoothProbe;
import edu.mit.media.funf.probe.builtin.CallLogProbe;
import edu.mit.media.funf.probe.builtin.HardwareInfoProbe;
import edu.mit.media.funf.probe.builtin.LocationProbe;
import edu.mit.media.funf.probe.builtin.RunningApplicationsProbe;
import edu.mit.media.funf.probe.builtin.ScreenProbe;
import edu.mit.media.funf.probe.builtin.ServicesProbe;
import edu.mit.media.funf.probe.builtin.TemperatureSensorProbe;
import xmind.nccu.edu.xmind_funf.R;
import xmind.nccu.edu.xmind_funf.Util.FunfDataBaseHelper;
import xmind.nccu.edu.xmind_funf.Util.GetCurrentRunningApp;
import xmind.nccu.edu.xmind_funf.Util.UploadUtil;
import xmind.nccu.edu.xmind_funf.Util.UploadingHelper;
import xmind.nccu.edu.xmind_funf.Util.UserLogUtil;

/**
 * @author sid.ku
 * @version 1.2
 * @Edit Sep. 1, 2015
 * @since Jun. 22, 2015
 */
public class XmindService extends Service implements Probe.DataListener {

    private static final String TAG = XmindService.class.getSimpleName();
    public static final boolean isRecordAppByActivityManager = false;//false == using accessibility in "all android version", true == 5.1(Acc...ity), 5.0 or 4.4(ActivityManger)
    public static final String PIPELINE_NAME = "XmindService";
    public static final String CHECK_POINT = "xmind_regular_check_point";
    public static final String SERVICE_PROBE = "xmind_service_probe_reminder";
    public static final String UPLOADING_REMINDER = "xmind_upload_data_reminder";
    public static final String CALLLOG_REMINDER = "xmind_upload_callLog_reminder";
    public static final String TAKE_PICTURE = "xmind_action_take_picture";
    public static final String FIRST_TIME_START_SERVICE = "xmind_FIRST_TIME_START_SERVICE";

    public static final int CREATE_NEW_DIRECTORY = 1073742080;

    private FunfManager funfManager;
    private BasicPipeline pipeline;

    //    private WifiProbe wifiProbe;//okay, but using wifiStatusReceiver instead.
    private BatteryProbe batteryProbe;//okay
    private BluetoothProbe bluetoothProbe;//okay
    private CallLogProbe callLogProbe;//okay
    private LocationProbe locationProbe;//okay
    private RunningApplicationsProbe runningApplicationsProbe;//okay, only working on 4.4 or previous version.
    private ScreenProbe screenProbe;//okay
    private ServicesProbe servicesProbe;//okay
    private TemperatureSensorProbe temperatureSensorProbe;//not working currently.

    private HardwareInfoProbe hardwareInfoProbe;

    private Context mContext;

    private boolean isAlreadyRunning = true;
    private boolean isNewPictureByReceiver = false;
    private boolean isScreenOn = true;

    private AlarmManager alarmManager;
    private PendingIntent pendingIntent_CheckPoint;
    private PendingIntent pendingIntent_ServiceProbe;
    private PendingIntent pendingIntent_uploading;
    private PendingIntent pendingIntent_calllog;

    private ArrayList<FileObserver> al_fo = new ArrayList<>();
    private final Handler handler = new Handler();

    //disable hardwareInfo probe if funf is already got it.
    private SharedPreferences funf_xmind_sp;
    private int callRecord = 0;

    private UploadingHelper mUploadingTask = null;

    /* *
     * Start from here if first time launch service.
     * */
    @Override
    public void onCreate() {
        super.onCreate();
        isAlreadyRunning = false;//set it false if first time running this app.
        funf_xmind_sp = UserLogUtil.GetSharedPreferencesForTimeControl(this);//get SharedPreference here.
        callRecord = funf_xmind_sp.getInt(UserLogUtil.getCallLog, 0);//Check call history, get zero if it's first time.

        bindService(new Intent(this, FunfManager.class), funfManagerConn, BIND_AUTO_CREATE);
    }

    /* *
     * Service will unregister all listener and stop all alarmManger if service has been stop.
     * */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (funfManager != null) {
            isAlreadyRunning = false;
            funfManager.disablePipeline(PIPELINE_NAME);

//            wifiProbe.unregisterListener(XmindService.this);
            bluetoothProbe.unregisterListener(XmindService.this);
            locationProbe.unregisterListener(XmindService.this);
            runningApplicationsProbe.unregisterListener(runningAppListener);
            screenProbe.unregisterListener(XmindService.this);
            if (servicesProbe != null)
                servicesProbe.unregisterListener(XmindService.this);
            if (batteryProbe != null)
                batteryProbe.unregisterListener(XmindService.this);
            temperatureSensorProbe.unregisterListener(XmindService.this);

            hardwareInfoProbe.unregisterListener(XmindService.this);

            alarmManager.cancel(pendingIntent_CheckPoint);//Cancel timer
            alarmManager.cancel(pendingIntent_ServiceProbe);//Cancel timer
            alarmManager.cancel(pendingIntent_uploading);//Cancel timer
            alarmManager.cancel(pendingIntent_calllog);//Cancel timer
            unbindService(funfManagerConn);
        }

        if (al_fo.size() > 0) {//Stop watching folder if service destroyed.
            for (int i = 0; i < al_fo.size(); i++) {
                al_fo.get(i).stopWatching();
            }
        }

        this.unregisterReceiver(wifiStatusReceiver);
    }

    /* *
     * Simplify the ProbeType by this method.
     * */
    private String getType(String targetType) {
        String result = "";
        if (targetType != null && !targetType.equals("")) {
            result = targetType.substring(targetType.lastIndexOf(".") + 1);
            result = result.substring(0, result.length() - 1);//Remove last character --> "
        }
        return result;
    }

    private ServiceConnection funfManagerConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            funfManager = ((FunfManager.LocalBinder) service).getManager();
            registerProbes(funfManager != null ? true : false);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            funfManager = null;
        }
    };

    /* *
     * enable wifi listener here and executive different probes according action event
     * */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mContext = this;

        //Check the message from 'XmindReceiver' as following:
        if (intent != null && intent.getAction() != null) {
            if (!isAlreadyRunning || intent.getAction().equals(FIRST_TIME_START_SERVICE) || !(funfManager != null)) {
                isAlreadyRunning = true;
                bindService(new Intent(this, FunfManager.class), funfManagerConn, BIND_AUTO_CREATE);
                setServiceCalendar();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        getBatteryStatus();
                    }
                }, 5000);//Check battery status after 10 seconds(waiting for FunfManager has been launcehd.)

                //Register GPS listener, wifi listener and enable them on first time.
                IntentFilter filter = new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
                this.registerReceiver(wifiStatusReceiver, filter);

                //Create FileObserver on first time, and check status on the following.
                setFileObserverStatus();//Stop watching folder if 'isNewPictureByReceiver' is true, otherwise, start watching.
            }

            if (intent.getAction().equals(CHECK_POINT)) {
//                Log.v(TAG, "Get action from checkpoint");
                getBatteryStatus();
//                getServiceStatus();

                if (isScreenOn) {
                    if (Build.VERSION.SDK_INT < 22 && isRecordAppByActivityManager) {
                        FunfDataBaseHelper FDB_Helper = new FunfDataBaseHelper(mContext, FunfDataBaseHelper.XMIND_FUNF_DATABASE_NAME);
                        GetCurrentRunningApp gcra = new GetCurrentRunningApp(mContext);
                        FDB_Helper.addCurrentForegroundAppRecord(FunfDataBaseHelper.CURRENT_FOREGROUND_APP, String.valueOf(System.currentTimeMillis()), gcra.getCurrentAppName());
                        FDB_Helper.close();
                    }
                }
            } else if (intent.getAction().equals(SERVICE_PROBE)) {
                getServiceStatus();
            } else if (intent.getAction().equals(UPLOADING_REMINDER)) {
                uploadingRecords();
            } else if (intent.getAction().equals(CALLLOG_REMINDER)) {
//                Log.v(TAG, "Get action for remind uploading.");
                getCallLogHistory();
            } else if (intent.getAction().equals(TAKE_PICTURE)) {
                //using file observer to get photo event, NEW_PICTURE action is useless currently.
                if (al_fo.size() == 0) {//no watcher!
                    FunfDataBaseHelper FDB_Helper = new FunfDataBaseHelper(mContext, FunfDataBaseHelper.XMIND_FUNF_DATABASE_NAME);
                    FDB_Helper.addPhotoRecord(FunfDataBaseHelper.TAKE_A_NEW_PHOTO_EVENT, String.valueOf(System.currentTimeMillis()));
                    FDB_Helper.close();
                }
                isNewPictureByReceiver = true;
                setFileObserverStatus();//Stop watching folder if 'isNewPictureByReceiver' is true, otherwise, start watching.
            }
        } else
            Log.e(TAG, "Got a null intent, wouldn't start service.");

        return super.onStartCommand(intent, flags, startId);
    }

    /* *
     * do something, after uploading success or not.
     * */
    private UploadingHelper.PostExecuteListener PostListner_SendingDataTask = new UploadingHelper.PostExecuteListener() {
        @Override
        public void onPostExecute(String result, boolean isDeleteAll, int uploadFirstNrows) {
//            Log.v(TAG, "Sent result : " + result);
            try {
                JSONObject js = new JSONObject(result);
                if (js.getString("state").toString().equals("true")) {
//                    Log.v(TAG, "Automatically uploading data succeed.");
                    if (isDeleteAll)
                        removeAll();
                    else
                        deleteFirstNRows(String.valueOf(uploadFirstNrows));
                }
            } catch (Exception e) {
//                Toast.makeText(mContext, "[NOT JSON OBJECT] :" + result, Toast.LENGTH_LONG).show();
            }
        }
    };

    /* *
     * Upload user record by this method.
     * */
    private void uploadingRecords() {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        boolean is3gAvailable = false;
        try {
            is3gAvailable = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnectedOrConnecting();
        } catch (Exception e) {
        }//Device is tablet computer.

        if (mWifi.isConnected() || is3gAvailable) {//Start uploading processing if Wifi or MobileData is available.
            if (mUploadingTask == null || !(mUploadingTask.getStatus() == AsyncTask.Status.RUNNING)) {
                mUploadingTask = new UploadingHelper(mContext);
                mUploadingTask.setPostExecuteListener(PostListner_SendingDataTask);
                try {
                    mUploadingTask.execute("http://mobilesns.cs.nccu.edu.tw/xmind-backend/bupload.php").get(10000, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    //Timeout
                }
            }
        }
    }

    /* *
    * Delete first N rows from Database after uploading success.
    * */
    public void deleteFirstNRows(String deleteNrows) {
        FunfDataBaseHelper FDB_Helper = new FunfDataBaseHelper(mContext, FunfDataBaseHelper.XMIND_FUNF_DATABASE_NAME);
        SQLiteDatabase db = FDB_Helper.getWritableDatabase();
        String ALTER_TBL = "delete from " + FunfDataBaseHelper.XMIND_FUNF_DATABASE_NAME +
                " where _id in (select _id from " + FunfDataBaseHelper.XMIND_FUNF_DATABASE_NAME + " order by _id LIMIT " + deleteNrows + ");";
        db.execSQL(ALTER_TBL);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                uploadingRecords();
            }
        }, 30000);//re-uploading other data after 30 seconds.
    }

    /* *
     * Delete all data in database
     * */
    private void removeAll() {
        deleteSingleDB(FunfDataBaseHelper.XMIND_FUNF_DATABASE_NAME);
        Log.v(TAG, "Remove all data from database...");
    }

    private void deleteSingleDB(String DBname) {
        FunfDataBaseHelper FDB_Device_Helper = new FunfDataBaseHelper(mContext, DBname);
        SQLiteDatabase db_device = FDB_Device_Helper.getWritableDatabase(); // helper is object extends SQLiteOpenHelper
        db_device.delete(DBname, null, null);
        db_device.close();
    }

    /* *
     * get wifi status change by this listener.
     *
     * @wifiTag: Wifi Tag : 0 == wifi has been turned on and connected; 1 == wifi has been turned off; 2 == wifi turned on, but no signal currently.
     * */
    private BroadcastReceiver wifiStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SupplicantState supState;
            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            supState = wifiInfo.getSupplicantState();
//            Log.w(TAG, "======== current supplicant state : " + supState);
            ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            boolean is3gAvailable = false;
            try {
                is3gAvailable = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnectedOrConnecting();
            } catch (Exception e) {
            }//Device is tablet computer.
            FunfDataBaseHelper FDB_Helper = new FunfDataBaseHelper(mContext, FunfDataBaseHelper.XMIND_FUNF_DATABASE_NAME);
            int wifiTag = -1;
            if (supState.equals(SupplicantState.COMPLETED)) {
                wifiTag = wifiManager.isWifiEnabled() ? 0 : 1;
//                Log.v("ssku", "Wifi changed. " + wifiTag);
                if (wifiTag == 0) {//0 == connected
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            uploadingRecords();
                        }
                    }, 20000);//Start uploading data after 5 seconds if wifi connected.
                }
            } else if (supState.equals(SupplicantState.DISCONNECTED)) {
                wifiTag = 2;
            }
            if (wifiTag != -1) {//only add wifi state when wifi connected or disconnected.
                FDB_Helper.addNetworkStateRecord(FunfDataBaseHelper.WIFI_STATUS_PROBE, String.valueOf(System.currentTimeMillis()), wifiTag, is3gAvailable);
            }
            FDB_Helper.close();
//            else {
////                WifiAlertDialogFragment.wifiCheck(HomeActivity.this);
//                if (supState.equals(SupplicantState.SCANNING)) {
//                    Log.d(TAG, "wifi scanning");
//                } else if (supState.equals(SupplicantState.DISCONNECTED)) {
//                    Log.d(TAG, "wifi disonnected");
//                } else {
//                    Log.d(TAG, "wifi connecting");
//                }
//            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /* *
     * get phone's battery status by this method
     * */
    public void getBatteryStatus() {
        if (funfManager != null) {
            Gson gson = funfManager.getGson();
            batteryProbe = gson.fromJson(new JsonObject(), BatteryProbe.class);
            batteryProbe.registerListener(XmindService.this);
            pipeline = (BasicPipeline) funfManager.getRegisteredPipeline(PIPELINE_NAME);
            funfManager.enablePipeline(PIPELINE_NAME);
        } else
            Log.e(TAG, "Enable battery's pipeline failed.");
    }

    /* *
     * get service's status by this method
     * */
    public void getServiceStatus() {
        if (funfManager != null) {
            Gson gson = funfManager.getGson();
            servicesProbe = gson.fromJson(new JsonObject(), ServicesProbe.class);
            servicesProbe.registerListener(XmindService.this);
            pipeline = (BasicPipeline) funfManager.getRegisteredPipeline(PIPELINE_NAME);
            funfManager.enablePipeline(PIPELINE_NAME);
        } else
            Log.e(TAG, "Enable ServiceProbe's pipeline failed.");
    }

    /* *
     * get call hsitory status by this method
     * */
    public void getCallLogHistory() {
        if (funfManager != null) {
            Gson gson = funfManager.getGson();
            callLogProbe = gson.fromJson(new JsonObject(), CallLogProbe.class);
            callLogProbe.registerListener(XmindService.this);
            pipeline = (BasicPipeline) funfManager.getRegisteredPipeline(PIPELINE_NAME);
            funfManager.enablePipeline(PIPELINE_NAME);
//            Log.v(TAG, "After get callLog method...");
        } else
            Log.e(TAG, "Enable ServiceProbe's pipeline failed.");
    }

    /* *
     * set the frequency(Checking) of all probes here.
     * */
    private void setServiceCalendar() {
        //Set regular checkpoint calendar:
        Intent ChcekPointIntent = new Intent(mContext, XmindReceiver.class);
        ChcekPointIntent.setAction(CHECK_POINT);
        pendingIntent_CheckPoint = PendingIntent.getBroadcast(mContext, 0, ChcekPointIntent, 0);
        alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.SECOND, 60);
        long frequency = mContext.getResources().getInteger(R.integer.timer_checkpoint_frequency) * 60 * 1000;//One minute.
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), frequency, pendingIntent_CheckPoint);

        //Set ServiceProbe calendar,(Disable this calendar, since the frequency is same with CheckPoint.(Do it together with CheckPoint)):
        Intent ServiceProbeIntent = new Intent(mContext, XmindReceiver.class);
        ServiceProbeIntent.setAction(SERVICE_PROBE);
        pendingIntent_ServiceProbe = PendingIntent.getBroadcast(mContext, 0, ServiceProbeIntent, 0);
        alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        Calendar ServiceProbeCalendar = Calendar.getInstance();
        ServiceProbeCalendar.setTimeInMillis(System.currentTimeMillis());
        ServiceProbeCalendar.add(Calendar.SECOND, 60);
        long serviceProvefrequency = mContext.getResources().getInteger(R.integer.timer_service_prove_frequency) * 60 * 1000;//One minute.
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, ServiceProbeCalendar.getTimeInMillis(), serviceProvefrequency, pendingIntent_ServiceProbe);

        //Set uploading calendar:
        Intent UploadingIntent = new Intent(mContext, XmindReceiver.class);
        UploadingIntent.setAction(UPLOADING_REMINDER);
        pendingIntent_uploading = PendingIntent.getBroadcast(mContext, 0, UploadingIntent, 0);
        Calendar UploadingCalendar = Calendar.getInstance();
        UploadingCalendar.setTimeInMillis(System.currentTimeMillis());
        UploadingCalendar.add(Calendar.SECOND, 60);
        long uploadingFrequency = mContext.getResources().getInteger(R.integer.timer_uploading_frequency) * 60 * 1000;
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, UploadingCalendar.getTimeInMillis(), uploadingFrequency, pendingIntent_uploading);

        //Set call log calendar:
        Intent CallLogIntent = new Intent(mContext, XmindReceiver.class);
        CallLogIntent.setAction(CALLLOG_REMINDER);
        pendingIntent_calllog = PendingIntent.getBroadcast(mContext, 0, CallLogIntent, 0);
        Calendar CallLogCalendar = Calendar.getInstance();
        CallLogCalendar.setTimeInMillis(System.currentTimeMillis());
        CallLogCalendar.add(Calendar.SECOND, 60);
        long CallLogFrequency = mContext.getResources().getInteger(R.integer.timer_calllog_frequency) * 60 * 1000;
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, CallLogCalendar.getTimeInMillis(), CallLogFrequency, pendingIntent_calllog);
    }

    /* *
     * Active all probes and register listener here
     * */
    private void registerProbes(boolean funfManagerIsNotNull) {
        if (funfManagerIsNotNull) {
            Gson gson = funfManager.getGson();
            bluetoothProbe = gson.fromJson(new JsonObject(), BluetoothProbe.class);
            locationProbe = gson.fromJson(new JsonObject(), LocationProbe.class);
            runningApplicationsProbe = gson.fromJson(new JsonObject(), RunningApplicationsProbe.class);
            screenProbe = gson.fromJson(new JsonObject(), ScreenProbe.class);
            temperatureSensorProbe = gson.fromJson(new JsonObject(), TemperatureSensorProbe.class);
            hardwareInfoProbe = gson.fromJson(new JsonObject(), HardwareInfoProbe.class);

            pipeline = (BasicPipeline) funfManager.getRegisteredPipeline(PIPELINE_NAME);

            bluetoothProbe.registerPassiveListener(XmindService.this);
            locationProbe.registerListener(XmindService.this);//new added
            locationProbe.registerPassiveListener(XmindService.this);//original
            runningApplicationsProbe.registerListener(runningAppListener);
            screenProbe.registerPassiveListener(XmindService.this);
            temperatureSensorProbe.registerPassiveListener(XmindService.this);
            hardwareInfoProbe.registerListener(XmindService.this);

            funfManager.enablePipeline(PIPELINE_NAME);
        } else
            Log.v(TAG, "(On X-mind service)funfManager is NULL.");
    }

    private Probe.DataListener runningAppListener = new Probe.DataListener() {
        public void onDataReceived(IJsonObject completeProbeUri, IJsonObject data) {
//            Log.v(TAG, "(1)(On X-mind service)RunningApplications: " + data);

        }

        public void onDataCompleted(IJsonObject completeProbeUri, JsonElement checkpoint) {
//            Log.w(TAG, "((2)On X-mind service)RunningApplications service has beend disable.");
        }
    };

    /* *
     * get status changed events here.
     * */
    @Override
    public void onDataReceived(IJsonObject iJsonObject, IJsonObject iJsonObject1) {
//        Log.i(TAG, "(3)Get event : " + getType(iJsonObject.get("@type").toString()) + ", data : " + iJsonObject1.toString());

        FunfDataBaseHelper FDB_Helper = new FunfDataBaseHelper(mContext, FunfDataBaseHelper.XMIND_FUNF_DATABASE_NAME);
        switch (getType(iJsonObject.get("@type").toString())) {
            case UploadUtil.HARDWARE_INFO_PROBE:
                if (funf_xmind_sp.getBoolean(UserLogUtil.getHardwareInfo, true)) {//record it if it's first time.
                    FunfDataBaseHelper FDB_Helper_Device = new FunfDataBaseHelper(mContext, FunfDataBaseHelper.XMIND_FUNF_DATABASE_DEVICE);
                    String Model = "";
                    String DeviceID = "";
                    if (iJsonObject1.get("model") != null)
                        Model = iJsonObject1.get("model").toString().replaceAll("[^a-zA-Z0-9]+", "");
                    if (iJsonObject1.get("deviceId") != null)
                        DeviceID = iJsonObject1.get("deviceId").toString().replaceAll("[^a-zA-Z0-9]+", "");
                    FDB_Helper_Device.addHardwareInfo(getType(iJsonObject.get("@type").toString()), String.valueOf(System.currentTimeMillis()), Model, DeviceID);
                    FDB_Helper_Device.close();
                    funf_xmind_sp.edit().putBoolean(UserLogUtil.getHardwareInfo, false).apply();
                }
                break;
            case UploadUtil.BATTERY_PROBE:
                FDB_Helper.addBatteryRecord(getType(iJsonObject.get("@type").toString()), String.valueOf(System.currentTimeMillis()), iJsonObject1.get("level").toString());
                break;
            case UploadUtil.BLUETOOTH_PROBE:
                FDB_Helper.addBluetoothRecord(getType(iJsonObject.get("@type").toString()), String.valueOf(System.currentTimeMillis()), iJsonObject1.get("android.bluetooth.device.extra.RSSI").toString());
                break;
            case UploadUtil.SERVICE_PROBE:
                String process = iJsonObject1.get("process").toString().replaceAll("[^a-zA-Z0-9.]+", "");
                FDB_Helper.addServiceRecord(getType(iJsonObject.get("@type").toString()), String.valueOf(System.currentTimeMillis()), process);
                break;
            case UploadUtil.SCREEN_PROBE:
                FDB_Helper.addScreenRecord(getType(iJsonObject.get("@type").toString()), String.valueOf(System.currentTimeMillis()), iJsonObject1.get("screenOn").toString());

                if (iJsonObject1.get("screenOn").toString().equals("true")) {
                    isScreenOn = true;//Only record it on screen on.
                    if (Build.VERSION.SDK_INT < 22 && isRecordAppByActivityManager) {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                FunfDataBaseHelper FDB_Helper = new FunfDataBaseHelper(mContext, FunfDataBaseHelper.XMIND_FUNF_DATABASE_NAME);
                                //NEW_Picture, app name, time
                                GetCurrentRunningApp gcra = new GetCurrentRunningApp(mContext);
                                FDB_Helper.addCurrentForegroundAppRecord(FunfDataBaseHelper.CURRENT_FOREGROUND_APP_AFTER_SCREEN_UNLOCK, String.valueOf(System.currentTimeMillis()), gcra.getCurrentAppName());
                                FDB_Helper.close();
                            }
                        }, 10000);//Detect foreground application after screen unlock 10's.
                    }
                } else
                    isScreenOn = false;

                break;
            case UploadUtil.LOCATION_PROBE:
                FDB_Helper.addLocationRecord(getType(iJsonObject.get("@type").toString()), String.valueOf(System.currentTimeMillis()), iJsonObject1.get("mLatitude").toString(), iJsonObject1.get("mLongitude").toString());
                break;
            case UploadUtil.CALLLOG_PROBE:
                try {
                    int id = Integer.parseInt(iJsonObject1.get("_id").toString());
//                    Log.v(TAG, "ID : " + id);
                    if (id > callRecord) {
                        callRecord = id;
                    }
                    if (id > funf_xmind_sp.getInt(UserLogUtil.getCallLog, 0)) {
                        FDB_Helper.addCallLogRecord(getType(iJsonObject.get("@type").toString()), String.valueOf(System.currentTimeMillis()), Integer.parseInt(iJsonObject1.get("duration").toString()), iJsonObject1.get("timestamp").toString());
//                        Log.v(TAG, "After add a new call log, Type : " + getType(iJsonObject.get("@type").toString()) + ", Time : " + String.valueOf(System.currentTimeMillis()) + ", Duration : " + iJsonObject1.get("duration").toString() + ", Timestamp : " + iJsonObject1.get("timestamp").toString());
                    }
                } catch (Exception e) {
                }
                break;
        }
        FDB_Helper.close();
    }

    //call it, when user disable the pipeline and unregister the DataListener.
    @Override
    public void onDataCompleted(IJsonObject iJsonObject, JsonElement jsonElement) {
//        Log.i(TAG, "(4)(On X-mind service)The probe [" + iJsonObject.get("@type") + "] has been disable service.");
        switch (getType(iJsonObject.get("@type").toString())) {
            case UploadUtil.CALLLOG_PROBE:
                funf_xmind_sp.edit().putInt(UserLogUtil.getCallLog, callRecord).apply();
                break;
        }
    }

    /* *
     * Add FileObserver for listen if user take a new photo
     * */
    private FileObserver addFileObserver(String path) {
        FileObserver observer = new FileObserver(path) { // set up a file observer to watch the DCIM directory
            @Override
            public void onEvent(int event, String file) {
                //Observing a specific folder, and write DB if there is any new file has been created.
                if ((event == FileObserver.CREATE || event == CREATE_NEW_DIRECTORY) && !file.equals(".probe")) { // check if its a "create" and not equal to .probe because thats created every time camera is started
//                    Log.d(TAG, "===========File created [" + android.os.Environment.getExternalStorageDirectory().toString() + "/DCIM/100MEDIA/" + file + "]");
                    FunfDataBaseHelper FDB_Helper = new FunfDataBaseHelper(mContext, FunfDataBaseHelper.XMIND_FUNF_DATABASE_NAME);
                    FDB_Helper.addPhotoRecord(FunfDataBaseHelper.TAKE_A_NEW_PHOTO_EVENT, String.valueOf(System.currentTimeMillis()));
                    FDB_Helper.close();
                    if (Build.VERSION.SDK_INT < 22 && isRecordAppByActivityManager) {
                        handler.postDelayed(new Runnable() {//Check foreground app after 5 seconds when got new picture.
                            @Override
                            public void run() {
                                FunfDataBaseHelper FDB_Helper = new FunfDataBaseHelper(mContext, FunfDataBaseHelper.XMIND_FUNF_DATABASE_NAME);
                                //NEW_Picture, app name, time
                                GetCurrentRunningApp gcra = new GetCurrentRunningApp(mContext);
                                FDB_Helper.addCurrentForegroundAppRecord(FunfDataBaseHelper.CURRENT_FOREGROUND_APP_ON_NEW_PICUTR, String.valueOf(System.currentTimeMillis()), gcra.getCurrentAppName());
                                FDB_Helper.close();
                            }
                        }, 10000);//Delay 10 seconds for catch foreground app(who using camera to take picture).
                    }
                }
            }
        };
        observer.startWatching();
        return observer;
    }

    /* *
     * Enable or disable FileObserver depend on deveice
     * */
    private void setFileObserverStatus() {
        if (!isNewPictureByReceiver) {//FileObserver wouldn't active if we could get NEW_Picture action from XmindReceiver.
            if (al_fo.size() == 0) {
                /** Android default album path **/
                File f = new File(android.os.Environment.getExternalStorageDirectory().toString() + "/DCIM/100MEDIA");
                if (f.isDirectory())
                    al_fo.add(addFileObserver(android.os.Environment.getExternalStorageDirectory().toString() + "/DCIM/100MEDIA"));

                File f2 = new File(android.os.Environment.getExternalStorageDirectory().toString() + "/DCIM/Camera");
                if (f2.isDirectory())
                    al_fo.add(addFileObserver(android.os.Environment.getExternalStorageDirectory().toString() + "/DCIM/Camera"));

                //** FolderObserver list for SD card or other devices **/
                String[] sa = mContext.getResources().getStringArray(R.array.photos_observer_lise);
                for (int i = 0; i < sa.length; i++) {
                    if (new File(sa[i]).exists()){
                        al_fo.add(addFileObserver(sa[i]));
                    }
                }
            }
        } else {
            if (al_fo.size() > 0) {
                for (int i = 0; i < al_fo.size(); i++) {
                    al_fo.get(i).stopWatching();
                }
                al_fo.clear();
            }
        }
    }
}
