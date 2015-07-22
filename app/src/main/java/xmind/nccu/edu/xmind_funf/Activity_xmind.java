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
import android.widget.TextView;
import android.widget.Toast;

import com.androidquery.AQuery;

import java.util.ArrayList;

import xmind.nccu.edu.xmind_funf.Util.FunfDataBaseHelper;
import xmind.nccu.edu.xmind_funf.Util.ProbesObject;
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
                    Log.v(TAG, "Clicked.");
                    if (isServiceStart) {
                        mContext.stopService(new Intent(mContext, xmind_service.class));
                        isServiceStart = false;
                        showDataBastInListView();
                        Log.v(TAG, "Stop service");
                    } else {
                        aq.id(R.id.iv_iv_1).visible();
//                        iv_iv_1.setVisibility(View.VISIBLE);
                        aq.id(R.id.timelimits_main_listview).gone();
//                        timelimitst_listview.setVisibility(View.GONE);

                        Intent intent_initService = new Intent(mContext, xmind_service.class);
                        intent_initService.setAction(xmind_service.FIRST_TIME_START_SERVICE);

                        mContext.startService(intent_initService);
                        isServiceStart = true;
                        Log.v(TAG, "Start service");
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
        FunfDataBaseHelper FDB_Helper = new FunfDataBaseHelper(mContext, "data");
        Cursor cursor = FDB_Helper.selectDB();
        ArrayList<ProbesObject> al_ProbesObjects = new ArrayList<ProbesObject>();

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                ProbesObject po = new ProbesObject(cursor.getString(1), cursor.getString(2), cursor.getString(3));
                al_ProbesObjects.add(po);
                cursor.moveToNext();
            }
            cursor.close();
        }

        if (al_ProbesObjects.size() > 0) {
            mListViewAdapter = new ListViewAdapter(Activity_xmind.this, 0, al_ProbesObjects);
            aq.id(R.id.timelimits_main_listview).getListView().setAdapter(mListViewAdapter);
        } else
            Toast.makeText(mContext, "No records found in database", Toast.LENGTH_LONG).show();
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

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final String probeName = po.get(position).getProbeName();
            final String value = po.get(position).getValue();
            final String timestamp = po.get(position).getTimestamp();
            holder.tv_name_single_app_up.setText(probeName);
            holder.tv_name_single_app_main.setText(value);
            holder.tv_name_single_app_under.setText(timestamp);

            return convertView;
        }
    }

    private static class ViewHolder {
        public TextView tv_name_single_app_up;
        public TextView tv_name_single_app_main;
        public TextView tv_name_single_app_under;
    }

    public void removeAll() {
        FunfDataBaseHelper FDB_Helper = new FunfDataBaseHelper(mContext, "data");
        SQLiteDatabase db = FDB_Helper.getWritableDatabase(); // helper is object extends SQLiteOpenHelper
        db.delete("data", null, null);
        db.delete("data", null, null);
        db.close();
        aq.id(R.id.timelimits_main_listview).getListView().setAdapter(null);
        Toast.makeText(mContext, "===Delete all data===", Toast.LENGTH_LONG).show();
    }
}
