package xmind.nccu.edu.xmind_funf;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.androidquery.AQuery;

import java.util.ArrayList;

import xmind.nccu.edu.xmind_funf.Service.WindowChangeDetectingService;
import xmind.nccu.edu.xmind_funf.Service.XmindService;
import xmind.nccu.edu.xmind_funf.Util.FunfDataBaseHelper;
import xmind.nccu.edu.xmind_funf.Util.ProbesObject;
import xmind.nccu.edu.xmind_funf.Util.UploadUtil;
import xmind.nccu.edu.xmind_funf.Util.FunfHelper;

/**
 * @author sid.ku
 * @version 1.2
 * @Edit Aug. 20, 2015
 * @since Jun. 15, 2015
 */
public class ActivityUserLogApp extends Activity {

    private static final String TAG = ActivityUserLogApp.class.getSimpleName();

    public static final boolean isEnableUI = false;

    private AQuery aq;
    private Context mContext;
    private boolean isServiceStart = false;

    //UI display only
    private ListViewAdapter mListViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.act_xmind);

        mContext = this;

        //Start service automatically as following code---------
        Intent intent_initService = new Intent(mContext, XmindService.class);
        intent_initService.setAction(XmindService.FIRST_TIME_START_SERVICE);
//        intent_initService.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        mContext.startService(intent_initService);
        isServiceStart = true;

        if (Build.VERSION.SDK_INT > 21 || !(XmindService.isRecordAppByActivityManager)) {//Get foreground a
//            Intent intent_lollipopService = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
//            startActivityForResult(intent_lollipopService, 0);
            Intent intent_lollipopService = new Intent(mContext, WindowChangeDetectingService.class);
            mContext.startService(intent_lollipopService);
        }

        if(isEnableUI){
            aq = new AQuery(this);
            aq.id(R.id.btn_pipeline_controller).clicked(controller_ocl);
            aq.id(R.id.btn_clear_DB).clicked(controller_ocl);
            aq.id(R.id.iv_iv_1).background(R.drawable.gedetama2);
            aq.id(R.id.btn_clear_DB).enabled(false);
            updatePipelineStatus();
        }else
            finish();
    }

    private View.OnClickListener controller_ocl = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_pipeline_controller:
                    if (isServiceStart) {
                        mContext.stopService(new Intent(mContext, XmindService.class));
                        mContext.stopService(new Intent(mContext, WindowChangeDetectingService.class));
                        isServiceStart = false;
                        aq.id(R.id.tv_db_count).visible();
                        showDataBastInListView();
//                        printoutDB();
                    } else {
                            aq.id(R.id.iv_iv_1).visible();
                            aq.id(R.id.tv_db_count).gone();
                            aq.id(R.id.timelimits_main_listview).gone();
                            aq.id(R.id.btn_clear_DB).enabled(false);
                            Intent intent_initService = new Intent(mContext, XmindService.class);
                            intent_initService.setAction(XmindService.FIRST_TIME_START_SERVICE);

                            mContext.startService(intent_initService);
                            isServiceStart = true;
                    }
                    updatePipelineStatus();
                    break;
                case R.id.btn_clear_DB:
                    removeAll();//Clear DB
                    Log.w(TAG, "Database has been deleted.");
                    break;
            }
        }
    };

    /*

    for display data or record on UI, if service has been stoped.
     */
    private void showDataBastInListView() {
        aq.id(R.id.iv_iv_1).gone();
        aq.id(R.id.timelimits_main_listview).visible();
        //Show DataBase if service is suspends.
        ArrayList<ProbesObject> al_ProbesObjects = new ArrayList<>();
        FunfDataBaseHelper FDB_Device_Helper = new FunfDataBaseHelper(mContext, FunfDataBaseHelper.XMIND_FUNF_DATABASE_DEVICE);
        Cursor deviceCursor = FDB_Device_Helper.selectDeviceDB();
        if (deviceCursor.getCount() > 0) {
            deviceCursor.moveToLast();
            ProbesObject po = new ProbesObject();
            String type = deviceCursor.getString(1);
            po.setProbeName(type);
            po.setTimestamp(deviceCursor.getString(2));
            if (type.equals(UploadUtil.HARDWARE_INFO_PROBE)) {
                po.setModel(deviceCursor.getString(3));
                po.setDeviceId(deviceCursor.getString(4));
                aq.id(R.id.tv_device_info).text("Model - " + deviceCursor.getString(3) + ", ID : " + deviceCursor.getString(4));
            }
            deviceCursor.close();
            al_ProbesObjects.add(po);
        }

        FunfDataBaseHelper FDB_Helper = new FunfDataBaseHelper(mContext, FunfDataBaseHelper.XMIND_FUNF_DATABASE_NAME);
        Cursor cursor = FDB_Helper.selectDB();
        aq.id(R.id.tv_db_count).text("Total : " + (1 + cursor.getCount()));//two database
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                ProbesObject po = new ProbesObject();
                String type = cursor.getString(1);

                po.setProbeName(type);
                po.setTimestamp(cursor.getString(2));
                switch (type) {
                    case UploadUtil.WIFI_STATUS_PROBE:
                        po.setWifitag(cursor.getString(10));
                        po.setMobileData(cursor.getString(11));
                        break;
                    case UploadUtil.LOCATION_PROBE:
                        po.setLatitude(cursor.getString(5));
                        po.setLongitude(cursor.getString(6));
                        break;
                    case UploadUtil.BLUETOOTH_PROBE:
                        po.setRSSI(cursor.getString(7));
                        break;
                    case UploadUtil.SCREEN_PROBE:
                        po.setIsScreenOn(cursor.getString(8));
                        break;
                    case UploadUtil.TAKE_A_NEW_PHOTO_EVENT:
                        break;
                    case FunfDataBaseHelper.CURRENT_FOREGROUND_APP_AFTER_SCREEN_UNLOCK:
                        po.setPackageName(cursor.getString(9));
                        break;
                    case FunfDataBaseHelper.CURRENT_FOREGROUND_APP_ON_NEW_PICUTR:
                        po.setPackageName(cursor.getString(9));
                        break;
                    case FunfDataBaseHelper.CURRENT_FOREGROUND_APP:
                        po.setPackageName(cursor.getString(9));
                        break;
                    case UploadUtil.SERVICE_PROBE:
                        po.setProcess(cursor.getString(4));
                        break;
                    case UploadUtil.BATTERY_PROBE:
                        po.setBatteryLevel(cursor.getString(3));
                        break;
                    case UploadUtil.CALLLOG_PROBE:
                        po.setDuration(cursor.getString(12));
                        po.setCallDate(cursor.getString(13));
                        break;
                }
                al_ProbesObjects.add(po);
                cursor.moveToNext();
            }
            cursor.close();
        }

        if (al_ProbesObjects.size() > 0) {
            aq.id(R.id.btn_clear_DB).enabled(true);//Enable delete button if size not zero.
            mListViewAdapter = new ListViewAdapter(ActivityUserLogApp.this, 0, al_ProbesObjects);
            aq.id(R.id.timelimits_main_listview).getListView().setAdapter(mListViewAdapter);
        } else
            Toast.makeText(mContext, "No records found in database", Toast.LENGTH_LONG).show();
    }

    /*
    watching database via logcat
     */
    private void printoutDB() {
        FunfDataBaseHelper FDB_Helper = new FunfDataBaseHelper(mContext, FunfDataBaseHelper.XMIND_FUNF_DATABASE_NAME);
        Cursor cursor = FDB_Helper.selectDB();
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                Log.v(TAG, "Name : " + cursor.getString(1)
                                + " , timestamp : " + cursor.getString(2)
                                + " , batteryLevel : " + cursor.getString(3)
                                + " , process : " + cursor.getString(4)
                                + " , latitude : " + cursor.getString(5)
                                + " , longitude : " + cursor.getString(6)
                                + " , rssi : " + cursor.getString(7)
                                + " , isScreenOn : " + cursor.getString(8)
                                + " , packageName : " + cursor.getString(9)
                                + " , wifitag : " + cursor.getString(10)
                                + " , mobiletag : " + cursor.getString(11)
                );
                cursor.moveToNext();
            }
            cursor.close();
        }

        Log.w(TAG, "============================================================");

        FunfDataBaseHelper FDB_Device_Helper = new FunfDataBaseHelper(mContext, FunfDataBaseHelper.XMIND_FUNF_DATABASE_DEVICE);
        Cursor cursor_Device = FDB_Device_Helper.selectDeviceDB();
        if (cursor_Device.getCount() > 0) {
            cursor_Device.moveToFirst();
            for (int i = 0; i < cursor_Device.getCount(); i++) {
                Log.v(TAG, "Name : " + cursor_Device.getString(1)
                                + " , timestamp : " + cursor_Device.getString(2)
                                + " , model : " + cursor_Device.getString(3)
                                + " , deviceId : " + cursor_Device.getString(4)
                );
                cursor_Device.moveToNext();
            }
            FDB_Device_Helper.close();
        } else
            Log.e(TAG, "Device cursor is null.");
    }

    /*
    Update service status
     */
    private void updatePipelineStatus() {
        aq.id(R.id.tv_pipeline_status).text(isServiceStart ? "Service - Enabled" : "Service - Disable");
    }

    /*
    UI adapter
     */
    private class ListViewAdapter extends ArrayAdapter<ProbesObject> {
        private LayoutInflater inflater;
        private ArrayList<ProbesObject> po;

        public ListViewAdapter(Context context, int resource, ArrayList<ProbesObject> objects) {
            super(context, resource, objects);
            inflater = LayoutInflater.from(context);
            po = objects;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.probes_item, null);
                holder = new ViewHolder();
                holder.tv_name_single_app_up = (TextView) convertView.findViewById(R.id.tv_name_single_app_up);
                holder.tv_name_single_app_main = (TextView) convertView.findViewById(R.id.tv_name_single_app_main);
                holder.tv_name_single_app_under = (TextView) convertView.findViewById(R.id.tv_name_single_app_under);
                holder.iv_icon_single_app = (ImageView) convertView.findViewById(R.id.iv_icon_single_app);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final String probeName = po.get(position).getProbeName();
            final String timestamp = FunfHelper.getDate(po.get(position).getTimestamp());
            holder.tv_name_single_app_up.setText(probeName);
            holder.tv_name_single_app_under.setText(timestamp);


            switch (probeName) {
                case UploadUtil.WIFI_STATUS_PROBE:
                    holder.iv_icon_single_app.setImageResource(R.drawable.gadatama_1);
                    String wifiState = "";
                    switch (po.get(position).getWifitag()) {
                        case "0":
                            wifiState = "Wifi and MB connected";
                            break;
                        case "1":
                            wifiState = "Wifi connected only";
                            break;
                        case "2":
                            wifiState = "Wifi disconnected";
                            break;
                    }
                    final String Wifi_State = wifiState;
                    holder.tv_name_single_app_main.setText(Wifi_State);
                    break;
                case UploadUtil.LOCATION_PROBE:
                    holder.iv_icon_single_app.setImageResource(R.drawable.gadatama_2);
                    final String LocationProbe = po.get(position).getLatitude() + ", " + po.get(position).getLongitude();
                    holder.tv_name_single_app_main.setText(LocationProbe);
                    break;
                case UploadUtil.BLUETOOTH_PROBE:
                    holder.iv_icon_single_app.setImageResource(R.drawable.gadatama_3);
                    final String BluetoothProbe = "RSSI : " + po.get(position).getRSSI();
                    holder.tv_name_single_app_main.setText(BluetoothProbe);
                    break;
                case UploadUtil.SCREEN_PROBE:
                    holder.iv_icon_single_app.setImageResource(R.drawable.gadatama_4);
                    final String ScreenProbe = "isScreenOn : " + po.get(position).getIsScreenOn();
                    holder.tv_name_single_app_main.setText(ScreenProbe);
                    break;
                case UploadUtil.TAKE_A_NEW_PHOTO_EVENT:
                    holder.iv_icon_single_app.setImageResource(R.drawable.gadatama_5);
                    holder.tv_name_single_app_main.setText("");
                    break;
                case UploadUtil.HARDWARE_INFO_PROBE:
                    holder.iv_icon_single_app.setImageResource(R.drawable.gadatama_6);
                    final String HardwareInfoProbe = po.get(position).getModel() + ", " + po.get(position).getDeviceId();
                    holder.tv_name_single_app_main.setText(HardwareInfoProbe);
                    break;
                case FunfDataBaseHelper.CURRENT_FOREGROUND_APP_AFTER_SCREEN_UNLOCK:
                    holder.iv_icon_single_app.setImageResource(R.drawable.gadatama_7);
                    final String Current_ForeGround_Screen_Unlock_AppName = "App : " + po.get(position).getPackageName();
                    holder.tv_name_single_app_main.setText(Current_ForeGround_Screen_Unlock_AppName);
                    break;
                case FunfDataBaseHelper.CURRENT_FOREGROUND_APP_ON_NEW_PICUTR:
                    holder.iv_icon_single_app.setImageResource(R.drawable.gadatama_7);
                    final String Current_ForeGround_Camera_AppName = "App : " + po.get(position).getPackageName();
                    holder.tv_name_single_app_main.setText(Current_ForeGround_Camera_AppName);
                    break;
                case FunfDataBaseHelper.CURRENT_FOREGROUND_APP:
                    holder.iv_icon_single_app.setImageResource(R.drawable.gadatama_7);
                    final String Current_ForeGround_AppName = "App : " + po.get(position).getPackageName();
                    holder.tv_name_single_app_main.setText(Current_ForeGround_AppName);
                    break;
                case UploadUtil.SERVICE_PROBE:
                    holder.iv_icon_single_app.setImageResource(R.drawable.gadatama_8);
                    final String ServicesProbe = "Service : " + po.get(position).getProcess();
                    holder.tv_name_single_app_main.setText(ServicesProbe);
                    break;
                case UploadUtil.BATTERY_PROBE:
                    holder.iv_icon_single_app.setImageResource(R.drawable.gadatama_9);
                    final String BatteryProbe = "Battery status : " + po.get(position).getBatteryLevel();
                    holder.tv_name_single_app_main.setText(BatteryProbe);
                    break;
                case UploadUtil.CALLLOG_PROBE:
                    holder.iv_icon_single_app.setImageResource(R.drawable.gadatama_9);
                    final String CallLog = "Duration : " + po.get(position).getDuration() + ", Date : " + po.get(position).getCallDate();
                    holder.tv_name_single_app_main.setText(CallLog);
                    break;
            }

            return convertView;
        }
    }

    private static class ViewHolder {
        public TextView tv_name_single_app_up;
        public TextView tv_name_single_app_main;
        public TextView tv_name_single_app_under;
        public ImageView iv_icon_single_app;
    }

    /*
    Delete all data in database
     */
    private void removeAll() {
        deleteSingleDB(FunfDataBaseHelper.XMIND_FUNF_DATABASE_NAME);
        deleteSingleDB(FunfDataBaseHelper.XMIND_FUNF_DATABASE_DEVICE);

        aq.id(R.id.timelimits_main_listview).getListView().setAdapter(null);
        aq.id(R.id.btn_clear_DB).enabled(false);
        aq.id(R.id.tv_db_count).text("No data currently.");
        Toast.makeText(mContext, "===Delete all data===", Toast.LENGTH_SHORT).show();
    }

    private void deleteSingleDB(String DBname) {
        FunfDataBaseHelper FDB_Device_Helper = new FunfDataBaseHelper(mContext, DBname);
        SQLiteDatabase db_device = FDB_Device_Helper.getWritableDatabase(); // helper is object extends SQLiteOpenHelper
        db_device.delete(DBname, null, null);
        db_device.close();
    }
}
