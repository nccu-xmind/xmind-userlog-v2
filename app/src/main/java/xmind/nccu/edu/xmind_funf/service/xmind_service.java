package xmind.nccu.edu.xmind_funf.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.FileObserver;
import android.os.IBinder;
import android.util.Log;

import com.androidquery.AQuery;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
import edu.mit.media.funf.probe.builtin.WifiProbe;
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

    private WifiProbe wifiProbe;//okay
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

    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;

    private NameValueDatabaseHelper mNameValueDatabaseHelper;

    private FileObserver observer = null;

    @Override
    public void onCreate() {
        super.onCreate();
        isAlreadyRunning = false;
        Log.v(TAG, "First time running this service...");
    }

    //STOP - service and unregister listener
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (funfManager != null) {
            Log.v(TAG, "(On X-mind service)Prepare to disable pipeline and alarmManager");
            funfManager.disablePipeline(PIPELINE_NAME);

            wifiProbe.unregisterListener(xmind_service.this);
            bluetoothProbe.unregisterListener(xmind_service.this);
            callLogProbe.unregisterListener(xmind_service.this);
            locationProbe.unregisterListener(xmind_service.this);
            runningApplicationsProbe.unregisterListener(runningAppListener);
            screenProbe.unregisterListener(xmind_service.this);
            if(servicesProbe != null)
                servicesProbe.unregisterListener(xmind_service.this);
            if(batteryProbe != null)
                batteryProbe.unregisterListener(xmind_service.this);
            temperatureSensorProbe.unregisterListener(xmind_service.this);

            hardwareInfoProbe.unregisterListener(xmind_service.this);

            alarmManager.cancel(pendingIntent);//Cancel timer
            unbindService(funfManagerConn);
        }
        if(observer != null){
            observer.stopWatching();
        }
    }

    private String getType(String targetType){
        String result = "";
        if(targetType != null && !targetType.equals("")){
            result = targetType.substring(targetType.lastIndexOf(".") + 1);
            result = result.substring(0, result.length()-1);//Remove last character --> "
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
        if(intent != null && intent.getAction() != null){
            if (!isAlreadyRunning && intent.getAction().equals(FIRST_TIME_START_SERVICE)) {
                Log.v(TAG, "Prepare to start service.");
                isAlreadyRunning = true;
                bindService(new Intent(this, FunfManager.class), funfManagerConn, BIND_AUTO_CREATE);
                setServiceCalendar();
            } else if (intent != null && intent.getAction().equals(CHECK_POINT)) {
                Log.v(TAG, "Get action from checkpoint");
                getBatteryStatus();
                getServiceStatus();

                GetCurrentRunningApp gcra = new GetCurrentRunningApp(mContext);
            } else if(intent != null && intent.getAction().equals(TAKE_PICTURE)){
                GetCurrentRunningApp gcra = new GetCurrentRunningApp(mContext);
                //TODO write db
            }
        }else
            Log.e(TAG, "Error, Start service with issue.");

        observer = new FileObserver(android.os.Environment.getExternalStorageDirectory().toString() + "/DCIM/100MEDIA") { // set up a file observer to watch the DCIM directory
            @Override
            public void onEvent(int event, String file) {
                //Observing a specific folder, and write DB if there is any new file has been created.
                if(event == FileObserver.CREATE && !file.equals(".probe")){ // check if its a "create" and not equal to .probe because thats created every time camera is started
//                    Log.d(TAG, "===========File created [" + android.os.Environment.getExternalStorageDirectory().toString() + "/DCIM/100MEDIA/" + file + "]");
                    GetCurrentRunningApp gcra = new GetCurrentRunningApp(mContext);//get current app's name.
                    FunfDataBaseHelper FDB_Helper = new FunfDataBaseHelper(mContext, "data");
                    //NEW_Picture, app name, time
                    FDB_Helper.addLog("NEW_Picture", gcra.getCurrentAppName(), String.valueOf(System.currentTimeMillis()));
//                    Log.v(TAG, "Current timestamp : " + System.currentTimeMillis());
                    FDB_Helper.close();
                }
            }
        };
        observer.startWatching();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void getBatteryStatus() {
        if (funfManager != null) {
            Log.v(TAG, "Prepare to enable battery's pipeline.");
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
            Log.v(TAG, "Prepare to enable ServiceProbe's pipeline.");
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
        calendar.add(Calendar.SECOND, 60); // first time
        long frequency = 60 * 1000; // in ms
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), frequency, pendingIntent);
        Log.v(TAG, "Alarm has been setting.");
    }

    private void registerProbes(boolean funfManagerIsNotNull) {
        if (funfManagerIsNotNull) {
            Log.v(TAG, "(On X-mind service)Prepare to enalbe pepeline and probes.");
            Gson gson = funfManager.getGson();
            wifiProbe = gson.fromJson(new JsonObject(), WifiProbe.class);
            bluetoothProbe = gson.fromJson(new JsonObject(), BluetoothProbe.class);
            callLogProbe = gson.fromJson(new JsonObject(), CallLogProbe.class);
            locationProbe = gson.fromJson(new JsonObject(), LocationProbe.class);
            runningApplicationsProbe = gson.fromJson(new JsonObject(), RunningApplicationsProbe.class);
            screenProbe = gson.fromJson(new JsonObject(), ScreenProbe.class);
            temperatureSensorProbe = gson.fromJson(new JsonObject(), TemperatureSensorProbe.class);
            hardwareInfoProbe = gson.fromJson(new JsonObject(), HardwareInfoProbe.class);

            pipeline = (BasicPipeline) funfManager.getRegisteredPipeline(PIPELINE_NAME);

            wifiProbe.registerPassiveListener(xmind_service.this);
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
            Log.v(TAG, "(1)(On X-mind service)RunningApplications: " + data);

        }

        public void onDataCompleted(IJsonObject completeProbeUri, JsonElement checkpoint) {
//            Log.w(TAG, "((2)On X-mind service)RunningApplications service has beend disable.");
        }
    };

    @Override
    public void onDataReceived(IJsonObject iJsonObject, IJsonObject iJsonObject1) {
//        Log.i(TAG, "(3)Get event : " + getType(iJsonObject.get("@type").toString()) + ", data : " + iJsonObject1.toString());

        FunfDataBaseHelper FDB_Helper = new FunfDataBaseHelper(mContext, "data");
        switch(getType(iJsonObject.get("@type").toString())){
            case "HardwareInfoProbe":
                FDB_Helper.addLog(getType(iJsonObject.get("@type").toString()), iJsonObject1.get("model").toString(), iJsonObject1.get("timestamp").toString());
                break;
            case "WifiProbe":
                FDB_Helper.addLog(getType(iJsonObject.get("@type").toString()), iJsonObject1.get("SSID").toString(), iJsonObject1.get("timestamp").toString());
                break;
            case "BatteryProbe":
                FDB_Helper.addLog(getType(iJsonObject.get("@type").toString()), iJsonObject1.get("level").toString(), iJsonObject1.get("timestamp").toString());
                break;
            case "BluetoothProbe":
                FDB_Helper.addLog(getType(iJsonObject.get("@type").toString()), iJsonObject1.get("android.bluetooth.device.extra.RSSI").toString(), iJsonObject1.get("timestamp").toString());
                break;
            case "ServicesProbe":
                FDB_Helper.addLog(getType(iJsonObject.get("@type").toString()), iJsonObject1.get("process").toString(), iJsonObject1.get("timestamp").toString());
                break;
            case "ScreenProbe":
                FDB_Helper.addLog(getType(iJsonObject.get("@type").toString()), iJsonObject1.get("screenOn").toString(), iJsonObject1.get("timestamp").toString());
                break;
            case "LocationProbe":
                FDB_Helper.addLog(getType(iJsonObject.get("@type").toString()), iJsonObject1.get("mLatitude").toString(), iJsonObject1.get("timestamp").toString());
                break;
        }
        FDB_Helper.close();
    }

    //call it, when user disable the pipeline and unregister the DataListener.
    @Override
    public void onDataCompleted(IJsonObject iJsonObject, JsonElement jsonElement) {
//        Log.i(TAG, "(4)(On X-mind service)The probe [" + iJsonObject.get("@type") + "] has been disable service.");
    }

}
