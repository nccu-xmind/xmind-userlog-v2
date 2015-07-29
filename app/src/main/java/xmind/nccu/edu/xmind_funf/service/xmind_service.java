package xmind.nccu.edu.xmind_funf.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.FileObserver;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.androidquery.AQuery;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;

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
import edu.mit.media.funf.storage.NameValueDatabaseHelper;
import xmind.nccu.edu.xmind_funf.GetCurrentRunningApp;
import xmind.nccu.edu.xmind_funf.Util.FunfDataBaseHelper;

/**
 * Created by sid.ku on 6/22/15.
 */
public class xmind_service extends Service implements Probe.DataListener {

    private static final String TAG = "ssku";//xmind_service.class.getSimpleName();
    private AQuery aq;
    public static final String PIPELINE_NAME = "xmind_service";
    public static final String CHECK_POINT = "xmind_regular_check_point";
    public static final String TAKE_PICTURE = "xmind_action_take_picture";
    public static final String FIRST_TIME_START_SERVICE = "xmind_FIRST_TIME_START_SERVICE";
    private FunfManager funfManager;
    private BasicPipeline pipeline;

//    private WifiProbe wifiProbe;//okay, but using wifiStatusReceiver instead.
    private BatteryProbe batteryProbe;//okay
    private BluetoothProbe bluetoothProbe;//okay
    private CallLogProbe callLogProbe;
    private LocationProbe locationProbe;//okay
    private RunningApplicationsProbe runningApplicationsProbe;//okay, only working on 4.4 or previous version.
    private ScreenProbe screenProbe;//okay
    private ServicesProbe servicesProbe;//okay
    private TemperatureSensorProbe temperatureSensorProbe;//not working currently.

    private HardwareInfoProbe hardwareInfoProbe;

    private Context mContext;

    private boolean isAlreadyRunning = true;
    private boolean isNewPictureByReceiver = false;

    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;

    private NameValueDatabaseHelper mNameValueDatabaseHelper;

    //    private FileObserver observer = null;
    private ArrayList<FileObserver> al_fo = new ArrayList<>();
    private final Handler handler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
        isAlreadyRunning = false;//set it false if first time running this app.
    }

    //STOP - service and unregister listener
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (funfManager != null) {
            Log.v(TAG, "(On X-mind service)Prepare to disable pipeline and alarmManager");
            funfManager.disablePipeline(PIPELINE_NAME);

//            wifiProbe.unregisterListener(xmind_service.this);
            bluetoothProbe.unregisterListener(xmind_service.this);
            callLogProbe.unregisterListener(xmind_service.this);
            locationProbe.unregisterListener(xmind_service.this);
            runningApplicationsProbe.unregisterListener(runningAppListener);
            screenProbe.unregisterListener(xmind_service.this);
            if (servicesProbe != null)
                servicesProbe.unregisterListener(xmind_service.this);
            if (batteryProbe != null)
                batteryProbe.unregisterListener(xmind_service.this);
            temperatureSensorProbe.unregisterListener(xmind_service.this);

            hardwareInfoProbe.unregisterListener(xmind_service.this);

            alarmManager.cancel(pendingIntent);//Cancel timer
            unbindService(funfManagerConn);
        }

        if (al_fo.size() > 0) {//Stop watching folder if service destroyed.
            for (int i = 0; i < al_fo.size(); i++) {
                al_fo.get(i).stopWatching();
            }
        }

        this.unregisterReceiver(wifiStatusReceiver);
    }

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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mContext = this;

        //Register GPS listener, wifi listener and enable them on first time.
        if (!isAlreadyRunning) {
            Log.v(TAG, "First time --- create wifi and location service.");
            //TODO Testing...Get event if wifi state has been changed.
            IntentFilter filter = new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
            this.registerReceiver(wifiStatusReceiver, filter);

            //TODO GPS listener is ready.
//            final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//            lm.addGpsStatusListener(new android.location.GpsStatus.Listener() {
//                public void onGpsStatusChanged(int event) {
////                Log.v(TAG, "Event : " + event);
//                    switch (event) {
//                        case 1://GPS Started
//                            Toast.makeText(mContext, "===GPS system has been triggered(Testing)===", Toast.LENGTH_LONG).show();
//                            break;
//                        case 2://GPS Stoped
//                            break;
//                        case 4://GPS Working(e.g. Open Google map and watching it.)
//                            break;
//
//                    }
//                }
//            });
        } else
            Log.w(TAG, "Not first time, wouldn't enable the two service again.");

        if (intent != null && intent.getAction() != null) {
            if (!isAlreadyRunning && intent.getAction().equals(FIRST_TIME_START_SERVICE)) {
                Log.v(TAG, "Prepare to start service.");
                isAlreadyRunning = true;
                bindService(new Intent(this, FunfManager.class), funfManagerConn, BIND_AUTO_CREATE);
                setServiceCalendar();
                getBatteryStatus();
            } else if (intent != null && intent.getAction().equals(CHECK_POINT)) {
                Log.v(TAG, "Get action from checkpoint");
                getBatteryStatus();
                getServiceStatus();

                GetCurrentRunningApp gcra = new GetCurrentRunningApp(mContext);
                FunfDataBaseHelper FDB_Helper = new FunfDataBaseHelper(mContext, FunfDataBaseHelper.XMIND_FUNF_DATABASE_NAME);
                FDB_Helper.addCurrentForegroundAppRecord(FunfDataBaseHelper.CURRENT_FOREGROUND_APP, String.valueOf(System.currentTimeMillis()), gcra.getCurrentAppName());
                FDB_Helper.close();
            } else if (intent != null && intent.getAction().equals(TAKE_PICTURE)) {
                //using file observer to get photo event, NEW_PICTURE action is useless currently.
                FunfDataBaseHelper FDB_Helper = new FunfDataBaseHelper(mContext, FunfDataBaseHelper.XMIND_FUNF_DATABASE_NAME);
                FDB_Helper.addPhotoRecord(FunfDataBaseHelper.TAKE_A_NEW_PHOTO_EVENT, String.valueOf(System.currentTimeMillis()));
                FDB_Helper.close();
                isNewPictureByReceiver = true;
            }
        } else
            Log.e(TAG, "Error, Start service with issue.");

        //Create FileObserver on first time, and check status on the following.
        setFileObserverStatus();//Stop watching folder if 'isNewPictureByReceiver' is true, otherwise, start watching.

        return super.onStartCommand(intent, flags, startId);
    }

    //Wifi Tag : 0 == wifi has been turned on and connected; 1 == wifi has been turned off; 2 == wifi turned on, but no signal currently.
    private BroadcastReceiver wifiStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SupplicantState supState;
            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            supState = wifiInfo.getSupplicantState();
//            Log.w(TAG, "======== current supplicant state : " + supState);
            ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            boolean is3gAvailable = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnectedOrConnecting();
            FunfDataBaseHelper FDB_Helper = new FunfDataBaseHelper(mContext, FunfDataBaseHelper.XMIND_FUNF_DATABASE_NAME);
            int wifiTag = -1;
            if (supState.equals(SupplicantState.COMPLETED)) {
                wifiTag = wifiManager.isWifiEnabled() ? 0 : 1;
            } else if(supState.equals(SupplicantState.DISCONNECTED)){
                wifiTag = 2;
            }
            Log.v(TAG, "Wifi status changed : " + wifiTag);
            FDB_Helper.addNetworkStateRecord(FunfDataBaseHelper.TAKE_A_NEW_PHOTO_EVENT, String.valueOf(System.currentTimeMillis()), wifiTag, is3gAvailable);
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

    public void getBatteryStatus() {
        if (funfManager != null) {
            Gson gson = funfManager.getGson();
            batteryProbe = gson.fromJson(new JsonObject(), BatteryProbe.class);
            batteryProbe.registerListener(xmind_service.this);
            pipeline = (BasicPipeline) funfManager.getRegisteredPipeline(PIPELINE_NAME);
            funfManager.enablePipeline(PIPELINE_NAME);
        } else
            Log.e(TAG, "Enable battery's pipeline failed.");
    }

    public void getServiceStatus() {
        if (funfManager != null) {
            Gson gson = funfManager.getGson();
            servicesProbe = gson.fromJson(new JsonObject(), ServicesProbe.class);
            servicesProbe.registerListener(xmind_service.this);
            pipeline = (BasicPipeline) funfManager.getRegisteredPipeline(PIPELINE_NAME);
            funfManager.enablePipeline(PIPELINE_NAME);
        } else
            Log.e(TAG, "Enable ServiceProbe's pipeline failed.");
    }

    private void setServiceCalendar() {
        Intent myIntent = new Intent(mContext, receiver.class);
        myIntent.setAction(CHECK_POINT);
        pendingIntent = PendingIntent.getBroadcast(mContext, 0, myIntent, 0);

        alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.SECOND, 60);
        long frequency = 60 * 1000;
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), frequency, pendingIntent);
    }

    private void registerProbes(boolean funfManagerIsNotNull) {
        if (funfManagerIsNotNull) {
            Gson gson = funfManager.getGson();
//            wifiProbe = gson.fromJson(new JsonObject(), WifiProbe.class);
            bluetoothProbe = gson.fromJson(new JsonObject(), BluetoothProbe.class);
            callLogProbe = gson.fromJson(new JsonObject(), CallLogProbe.class);
            locationProbe = gson.fromJson(new JsonObject(), LocationProbe.class);
            runningApplicationsProbe = gson.fromJson(new JsonObject(), RunningApplicationsProbe.class);
            screenProbe = gson.fromJson(new JsonObject(), ScreenProbe.class);
            temperatureSensorProbe = gson.fromJson(new JsonObject(), TemperatureSensorProbe.class);
            hardwareInfoProbe = gson.fromJson(new JsonObject(), HardwareInfoProbe.class);

            pipeline = (BasicPipeline) funfManager.getRegisteredPipeline(PIPELINE_NAME);

//            wifiProbe.registerPassiveListener(xmind_service.this);
            bluetoothProbe.registerPassiveListener(xmind_service.this);
            callLogProbe.registerPassiveListener(xmind_service.this);
            locationProbe.registerPassiveListener(xmind_service.this);

            runningApplicationsProbe.registerListener(runningAppListener);

            screenProbe.registerPassiveListener(xmind_service.this);
            temperatureSensorProbe.registerPassiveListener(xmind_service.this);
//            temperatureSensorProbe.registerListener(xmind_service.this);
            hardwareInfoProbe.registerListener(xmind_service.this);

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

    @Override
    public void onDataReceived(IJsonObject iJsonObject, IJsonObject iJsonObject1) {
//        Log.i(TAG, "(3)Get event : " + getType(iJsonObject.get("@type").toString()) + ", data : " + iJsonObject1.toString());

        FunfDataBaseHelper FDB_Helper = new FunfDataBaseHelper(mContext, FunfDataBaseHelper.XMIND_FUNF_DATABASE_NAME);
        switch (getType(iJsonObject.get("@type").toString())) {
            case "HardwareInfoProbe"://TODO Only record once.
//                FDB_Helper.addHardwareInfo(getType(iJsonObject.get("@type").toString()), iJsonObject1.get("timestamp").toString(), iJsonObject1.get("model").toString(), iJsonObject1.get("deviceId").toString());
                break;
            //using wifiStatusReceiver instead.
//            case "WifiProbe":
//                FDB_Helper.addLog(getType(iJsonObject.get("@type").toString()), iJsonObject1.get("SSID").toString(), iJsonObject1.get("timestamp").toString());
//                break;
            case "BatteryProbe":
                FDB_Helper.addBatteryRecord(getType(iJsonObject.get("@type").toString()), iJsonObject1.get("timestamp").toString(), iJsonObject1.get("level").toString());
                break;
            case "BluetoothProbe":
                FDB_Helper.addBluetoothRecord(getType(iJsonObject.get("@type").toString()), iJsonObject1.get("timestamp").toString(), iJsonObject1.get("android.bluetooth.device.extra.RSSI").toString());
                break;
            case "ServicesProbe":
                FDB_Helper.addServiceRecord(getType(iJsonObject.get("@type").toString()), iJsonObject1.get("timestamp").toString(), iJsonObject1.get("process").toString());
                break;
            case "ScreenProbe":
                FDB_Helper.addScreenRecord(getType(iJsonObject.get("@type").toString()), iJsonObject1.get("timestamp").toString(), iJsonObject1.get("screenOn").toString());
                if (iJsonObject1.get("screenOn").toString().equals("true")) {//Only record it on screen on.
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            GetCurrentRunningApp gcra = new GetCurrentRunningApp(mContext);//get current app's name.
                            FunfDataBaseHelper FDB_Helper = new FunfDataBaseHelper(mContext, FunfDataBaseHelper.XMIND_FUNF_DATABASE_NAME);
                            //NEW_Picture, app name, time
                            FDB_Helper.addCurrentForegroundAppRecord(FunfDataBaseHelper.CURRENT_FOREGROUND_APP_AFTER_SCREEN_UNLICK, String.valueOf(System.currentTimeMillis()), gcra.getCurrentAppName());
                            FDB_Helper.close();
                        }
                    }, 10000);//Detect foreground application after screen unlock 10's.
                }
                break;
            case "LocationProbe":
                FDB_Helper.addLocationRecord(getType(iJsonObject.get("@type").toString()), iJsonObject1.get("timestamp").toString(), iJsonObject1.get("mLatitude").toString(), iJsonObject1.get("mLongitude").toString());
                break;
        }
        FDB_Helper.close();
    }

    //call it, when user disable the pipeline and unregister the DataListener.
    @Override
    public void onDataCompleted(IJsonObject iJsonObject, JsonElement jsonElement) {
//        Log.i(TAG, "(4)(On X-mind service)The probe [" + iJsonObject.get("@type") + "] has been disable service.");
    }

    private FileObserver addFileObserver(String path) {
        FileObserver observer = new FileObserver(path) { // set up a file observer to watch the DCIM directory
            @Override
            public void onEvent(int event, String file) {
                //Observing a specific folder, and write DB if there is any new file has been created.
                if (event == FileObserver.CREATE && !file.equals(".probe")) { // check if its a "create" and not equal to .probe because thats created every time camera is started
//                    Log.d(TAG, "===========File created [" + android.os.Environment.getExternalStorageDirectory().toString() + "/DCIM/100MEDIA/" + file + "]");
                    FunfDataBaseHelper FDB_Helper = new FunfDataBaseHelper(mContext, FunfDataBaseHelper.XMIND_FUNF_DATABASE_NAME);
                    FDB_Helper.addPhotoRecord(FunfDataBaseHelper.TAKE_A_NEW_PHOTO_EVENT, String.valueOf(System.currentTimeMillis()));
                    FDB_Helper.close();
                    handler.postDelayed(new Runnable() {//Check foreground app after 5 seconds when got new picture.
                        @Override
                        public void run() {
                            GetCurrentRunningApp gcra = new GetCurrentRunningApp(mContext);//get current app's name.
                            FunfDataBaseHelper FDB_Helper = new FunfDataBaseHelper(mContext, FunfDataBaseHelper.XMIND_FUNF_DATABASE_NAME);
                            //NEW_Picture, app name, time
                            Log.v(TAG, "==== new picture foreground app : " + gcra.getCurrentAppName());
                            FDB_Helper.addCurrentForegroundAppRecord(FunfDataBaseHelper.CURRENT_FOREGROUND_APP_ON_NEW_PICUTR, String.valueOf(System.currentTimeMillis()), gcra.getCurrentAppName());
                            FDB_Helper.close();
                        }
                    }, 5000);//Delay 5 seconds for catch foreground app(who using camera to take picture).
                }
            }
        };
        observer.startWatching();
        return observer;
    }

    private void setFileObserverStatus() {
        //TODO adjust logic here. using SP to check here.
        Log.v(TAG, "isNewPictureByReceiver : " + isNewPictureByReceiver + ", Size : " + al_fo.size());
        if (!isNewPictureByReceiver) {//FileObserver wouldn't active if we could get NEW_Picture action from receiver.
            if (al_fo.size() == 0) {
                Log.i(TAG, "FileObserver is Enabled.");
//            String albumPath = "";
                File f = new File(android.os.Environment.getExternalStorageDirectory().toString() + "/DCIM/100MEDIA");
                if (f.isDirectory()) {
                    al_fo.add(addFileObserver(android.os.Environment.getExternalStorageDirectory().toString() + "/DCIM/Came100MEDIAra"));
                } else
                    Log.e(TAG, "100MEDIA NOT exist");

                File f2 = new File(android.os.Environment.getExternalStorageDirectory().toString() + "/DCIM/Camera");
                if (f2.isDirectory()) {
                    al_fo.add(addFileObserver(android.os.Environment.getExternalStorageDirectory().toString() + "/DCIM/Camera"));
                } else
                    Log.e(TAG, "Camera NOT exist");
            } else {
                Log.w(TAG, "isNewPictureByReceiver is false, but arraylist is already exist.");
            }
        } else {
            if (al_fo.size() > 0) {
                for (int i = 0; i < al_fo.size(); i++) {
                    al_fo.get(i).stopWatching();
                }
                al_fo.clear();
                Log.i(TAG, "FileObserver is disabled, since we could get NEW_Picture action from receiver.");
            }
        }
    }
}
