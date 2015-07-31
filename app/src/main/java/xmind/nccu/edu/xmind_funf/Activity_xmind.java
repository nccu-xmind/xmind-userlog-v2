package xmind.nccu.edu.xmind_funf;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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

import xmind.nccu.edu.xmind_funf.Util.FunfDataBaseHelper;
import xmind.nccu.edu.xmind_funf.Util.ProbesObject;
import xmind.nccu.edu.xmind_funf.Util.funfHelper;
import xmind.nccu.edu.xmind_funf.service.xmind_service;

/**
 * @author sid.ku
 * @version 1.2
 * @Edit Jul. 21, 2015
 * @since Jun. 15, 2015
 */
public class Activity_xmind extends Activity {

    private static final String TAG = "ssku";//Activity_xmind.class.getSimpleName();

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
        aq = new AQuery(this);
        aq.id(R.id.btn_pipeline_controller).clicked(controller_ocl);
        aq.id(R.id.btn_clear_DB).clicked(controller_ocl);
        aq.id(R.id.iv_iv_1).background(R.drawable.gedetama2);
        aq.id(R.id.btn_clear_DB).enabled(false);

        Intent intent_initService = new Intent(mContext, xmind_service.class);
        intent_initService.setAction(xmind_service.FIRST_TIME_START_SERVICE);

        mContext.startService(intent_initService);
        isServiceStart = true;
        updatePipelineStatus();
    }

    private View.OnClickListener controller_ocl = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_pipeline_controller:
                    if (isServiceStart) {
                        mContext.stopService(new Intent(mContext, xmind_service.class));
                        isServiceStart = false;
                        aq.id(R.id.tv_db_count).visible();
                        showDataBastInListView();
//                        printoutDB();
                    } else {
                        aq.id(R.id.iv_iv_1).visible();
                        aq.id(R.id.tv_db_count).gone();
                        aq.id(R.id.timelimits_main_listview).gone();
                        aq.id(R.id.btn_clear_DB).enabled(false);
                        Intent intent_initService = new Intent(mContext, xmind_service.class);
                        intent_initService.setAction(xmind_service.FIRST_TIME_START_SERVICE);

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

    private void showDataBastInListView() {
        aq.id(R.id.iv_iv_1).gone();
        aq.id(R.id.timelimits_main_listview).visible();
        //Show DataBase if service is suspends.
        ArrayList<ProbesObject> al_ProbesObjects = new ArrayList<ProbesObject>();
        FunfDataBaseHelper FDB_Device_Helper = new FunfDataBaseHelper(mContext, FunfDataBaseHelper.XMIND_FUNF_DATABASE_DEVICE);
        Cursor deviceCursor = FDB_Device_Helper.selectDeviceDB();
        if (deviceCursor.getCount() > 0) {
            deviceCursor.moveToFirst();
            ProbesObject po = new ProbesObject();
            String type = deviceCursor.getString(1);
            po.setProbeName(type);
            po.setTimestamp(deviceCursor.getString(2));
            if (type.equals("HardwareInfoProbe")) {
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
                    case "Wifi_Status":
                        po.setWifitag(cursor.getString(10));
                        po.setMobileData(cursor.getString(11));
                        break;
                    case "LocationProbe":
                        po.setLatitude(cursor.getString(5));
                        po.setLongitude(cursor.getString(6));
                        break;
                    case "BluetoothProbe":
                        po.setRSSI(cursor.getString(7));
                        break;
                    case "ScreenProbe":
                        po.setIsScreenOn(cursor.getString(8));
                        break;
                    case "Take_a_New_Photo_Event":
                        break;
                    case "HardwareInfoProbe":
                        po.setProbeName(cursor.getString(1));
                        po.setTimestamp(cursor.getString(2));
                        break;
                    case "Current_ForeGround_Screen_Unlock_AppName":
                        po.setPackageName(cursor.getString(9));
                        break;
                    case "Current_ForeGround_Camera_AppName":
                        po.setPackageName(cursor.getString(9));
                        break;
                    case "Current_ForeGround_AppName":
                        po.setPackageName(cursor.getString(9));
                        break;
                    case "ServicesProbe":
                        po.setProcess(cursor.getString(4));
                        break;
                    case "BatteryProbe":
                        po.setBatteryLevel(cursor.getString(3));
                        break;
                }
                al_ProbesObjects.add(po);
                cursor.moveToNext();
            }
            cursor.close();
        }

        if (al_ProbesObjects.size() > 0) {
            aq.id(R.id.btn_clear_DB).enabled(true);//Enable delete button if size not zero.
            mListViewAdapter = new ListViewAdapter(Activity_xmind.this, 0, al_ProbesObjects);
            aq.id(R.id.timelimits_main_listview).getListView().setAdapter(mListViewAdapter);
        } else
            Toast.makeText(mContext, "No records found in database", Toast.LENGTH_LONG).show();
    }

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

    private void updatePipelineStatus() {
        aq.id(R.id.tv_pipeline_status).text(isServiceStart ? "Service - Enabled" : "Service - Disable");
    }

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

            int gadatama = position % 4;
            switch (gadatama) {
                case 0:
                    holder.iv_icon_single_app.setImageResource(R.drawable.gadatama_1);
                    break;
                case 1:
                    holder.iv_icon_single_app.setImageResource(R.drawable.gadatama_2);
                    break;
                case 2:
                    holder.iv_icon_single_app.setImageResource(R.drawable.gadatama_3);
                    break;
                case 3:
                    holder.iv_icon_single_app.setImageResource(R.drawable.gadatama_4);
                    break;
            }


            final String probeName = po.get(position).getProbeName();
            final String timestamp = funfHelper.getDate(po.get(position).getTimestamp());
            holder.tv_name_single_app_up.setText(probeName);
            holder.tv_name_single_app_under.setText(timestamp);


            switch (probeName) {
                case "Wifi_Status":
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
                case "LocationProbe":
                    final String LocationProbe = po.get(position).getLatitude() + ", " + po.get(position).getLongitude();
                    holder.tv_name_single_app_main.setText(LocationProbe);
                    break;
                case "BluetoothProbe":
                    final String BluetoothProbe = "RSSI : " + po.get(position).getRSSI();
                    holder.tv_name_single_app_main.setText(BluetoothProbe);
                    break;
                case "ScreenProbe":
                    final String ScreenProbe = "isScreenOn : " + po.get(position).getIsScreenOn();
                    holder.tv_name_single_app_main.setText(ScreenProbe);
                    break;
                case "Take_a_New_Photo_Event":
                    holder.tv_name_single_app_main.setText("");
                    break;
                case "HardwareInfoProbe":
                    final String HardwareInfoProbe = po.get(position).getModel() + ", " + po.get(position).getDeviceId();
                    holder.tv_name_single_app_main.setText(HardwareInfoProbe);
                    break;
                case "Current_ForeGround_Screen_Unlock_AppName":
                    final String Current_ForeGround_Screen_Unlock_AppName = "App : " + po.get(position).getPackageName();
                    holder.tv_name_single_app_main.setText(Current_ForeGround_Screen_Unlock_AppName);
                    break;
                case "Current_ForeGround_Camera_AppName":
                    final String Current_ForeGround_Camera_AppName = "App : " + po.get(position).getPackageName();
                    holder.tv_name_single_app_main.setText(Current_ForeGround_Camera_AppName);
                    break;
                case "Current_ForeGround_AppName":
                    final String Current_ForeGround_AppName = "App : " + po.get(position).getPackageName();
                    holder.tv_name_single_app_main.setText(Current_ForeGround_AppName);
                    break;
                case "ServicesProbe":
                    final String ServicesProbe = "Service : " + po.get(position).getProcess();
                    holder.tv_name_single_app_main.setText(ServicesProbe);
                    break;
                case "BatteryProbe":
                    final String BatteryProbe = "Battery status : " + po.get(position).getBatteryLevel();
                    holder.tv_name_single_app_main.setText(BatteryProbe);
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

    public void removeAll() {
        FunfDataBaseHelper FDB_Helper = new FunfDataBaseHelper(mContext, FunfDataBaseHelper.XMIND_FUNF_DATABASE_NAME);
        SQLiteDatabase db = FDB_Helper.getWritableDatabase(); // helper is object extends SQLiteOpenHelper
        db.delete(FunfDataBaseHelper.XMIND_FUNF_DATABASE_NAME, null, null);
        db.close();

        FunfDataBaseHelper FDB_Device_Helper = new FunfDataBaseHelper(mContext, FunfDataBaseHelper.XMIND_FUNF_DATABASE_DEVICE);
        SQLiteDatabase db_device = FDB_Device_Helper.getWritableDatabase(); // helper is object extends SQLiteOpenHelper
        db_device.delete(FunfDataBaseHelper.XMIND_FUNF_DATABASE_DEVICE, null, null);
        db_device.close();

        aq.id(R.id.timelimits_main_listview).getListView().setAdapter(null);
        aq.id(R.id.btn_clear_DB).enabled(false);
        aq.id(R.id.tv_db_count).text("No data currently.");
        Toast.makeText(mContext, "===Delete all data===", Toast.LENGTH_SHORT).show();
    }
}
