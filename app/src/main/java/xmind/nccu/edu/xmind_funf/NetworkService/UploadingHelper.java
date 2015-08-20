package xmind.nccu.edu.xmind_funf.NetworkService;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;

import com.androidquery.AQuery;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import xmind.nccu.edu.xmind_funf.Util.FunfDataBaseHelper;
import xmind.nccu.edu.xmind_funf.Util.UploadUtil;

/**
 * Created by sid.ku on 8/3/15.
 */
public class UploadingHelper extends AsyncTask<String, Void, String> {
    private static final String TAG = UploadingHelper.class.getSimpleName();

    public static final String STATUS_CODE_001 = "DatabaseIsEmpty";

    private AQuery aq;
    private Context mContext;
    private PostExecuteListener mPostExecuteListener;
    private Cursor dataCursor = null;
    private Cursor deviceCursor = null;

    private String primaryEMail = "";
    private String deviceID = "";
    private String deviceModel = "";
    private String UploadingTimestamp = "";

    public UploadingHelper(Context mContext) {
        this.mContext = mContext;
        FunfDataBaseHelper FDB_Helper = new FunfDataBaseHelper(mContext, FunfDataBaseHelper.XMIND_FUNF_DATABASE_NAME);
        FunfDataBaseHelper FDB_Device_Helper = new FunfDataBaseHelper(mContext, FunfDataBaseHelper.XMIND_FUNF_DATABASE_DEVICE);
        dataCursor = FDB_Helper.selectDB();
        deviceCursor = FDB_Device_Helper.selectDeviceDB();

        primaryEMail = UploadUtil.getDeviceEmail(mContext);
        ArrayList<String> al_device = new ArrayList<>();
        al_device = getDevice();
        if(al_device.size() > 0){
            deviceModel = al_device.get(0);
            deviceID = al_device.get(1);
        }
        UploadingTimestamp = String.valueOf(System.currentTimeMillis());
//        Log.v(TAG, "E-mail : " + primaryEMail + ", Device Model" + deviceModel + ", Device ID : " + deviceID + ", Current timestamp : " + UploadingTimestamp);
    }

    private ArrayList<String> getDevice() {
        ArrayList<String> al_device = new ArrayList<>();
        if (deviceCursor != null) {
            if (deviceCursor.getCount() > 0) {
                deviceCursor.moveToLast();
                al_device.add(deviceCursor.getString(3));//Model
                al_device.add(deviceCursor.getString(4));//ID
                deviceCursor.close();
            }
        }
        return al_device;
    }

    @Override
    protected void onPreExecute() {
//        Log.i(TAG, "Prepare sending the data to server...");
    }

    @Override
    protected String doInBackground(String... urls) {
        return setPostData(urls[0]);
    }

    // onPostExecute displays the results of the AsyncTask.
    @Override
    protected void onPostExecute(String result) {
        try {
            mPostExecuteListener.onPostExecute(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String setPostData(String url) {
        if (dataCursor.getCount() > 0) {
            InputStream inputStream = null;
            String result = "";
            try {
                HttpClient httpclient = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost(url);
                String json = "";

                JSONObject jsonData = new JSONObject();
                jsonData.accumulate(UploadUtil.OBJ_MAIL, primaryEMail);
                jsonData.accumulate(UploadUtil.OBJ_MODEL, deviceModel);
                jsonData.accumulate(UploadUtil.OBJ_DEVICE, deviceID);
                jsonData.accumulate(UploadUtil.OBJ_UPLOADING_TIME, UploadingTimestamp);

                JSONArray jasonProbeArray = new JSONArray();
                if (dataCursor.getCount() > 0) {
                    dataCursor.moveToFirst();
                    for (int i = 0; i < dataCursor.getCount(); i++) {
                        JSONObject jsonProbeChild = new JSONObject();
                        String probeType = dataCursor.getString(1);
                        jsonProbeChild.put(UploadUtil.OBJ_PROBE_TYPE, probeType);
                        jsonProbeChild.put(UploadUtil.OBJ_TIMESTAMP, dataCursor.getString(2));
                        switch (probeType) {
                            case UploadUtil.WIFI_STATUS_PROBE:
                                jsonProbeChild.put(UploadUtil.OBJ_NETWORK, dataCursor.getString(10));
                                break;
                            case UploadUtil.LOCATION_PROBE:
                                jsonProbeChild.put(UploadUtil.OBJ_LATITUDE, dataCursor.getString(5));
                                jsonProbeChild.put(UploadUtil.OBJ_LONGITUDE, dataCursor.getString(6));
                                break;
                            case UploadUtil.BLUETOOTH_PROBE:
                                jsonProbeChild.put(UploadUtil.OBJ_RSSI, dataCursor.getString(7));
                                break;
                            case UploadUtil.SCREEN_PROBE:
                                jsonProbeChild.put(UploadUtil.OBJ_SCREEN, dataCursor.getString(8));
                                break;
//                            case UploadUtil.TAKE_A_NEW_PHOTO_EVENT:
//                                break;
                            case FunfDataBaseHelper.CURRENT_FOREGROUND_APP_AFTER_SCREEN_UNLOCK:
                                jsonProbeChild.put(UploadUtil.OBJ_PACKAGENAME, dataCursor.getString(9));
                                break;
                            case FunfDataBaseHelper.CURRENT_FOREGROUND_APP_ON_NEW_PICUTR:
                                jsonProbeChild.put(UploadUtil.OBJ_PACKAGENAME, dataCursor.getString(9));
                                break;
                            case FunfDataBaseHelper.CURRENT_FOREGROUND_APP:
                                jsonProbeChild.put(UploadUtil.OBJ_PACKAGENAME, dataCursor.getString(9));
                                break;
                            case UploadUtil.SERVICE_PROBE:
                                jsonProbeChild.put(UploadUtil.OBJ_PROCESS, dataCursor.getString(4));
                                break;
                            case UploadUtil.BATTERY_PROBE:
                                jsonProbeChild.put(UploadUtil.OBJ_BATTERY, dataCursor.getString(3));
                                break;
                            case UploadUtil.CALLLOG_PROBE:
                                jsonProbeChild.put(UploadUtil.OBJ_DURATION, dataCursor.getString(12));
                                jsonProbeChild.put(UploadUtil.OBJ_DATE, dataCursor.getString(13));
                                break;
                        }
                        jasonProbeArray.put(jsonProbeChild);
                        dataCursor.moveToNext();
                    }
                    dataCursor.close();
                }

                jsonData.put(UploadUtil.PROBE_ARRAY, jasonProbeArray);

                json = jsonData.toString();
                Log.v(TAG, "json : " + json.toString());

                StringEntity se = new StringEntity(json);
                httpPost.setEntity(se);
                httpPost.setHeader("Content-type", "application/json");
                httpPost.setHeader("Accept", "application/json");

                HttpResponse httpResponse = httpclient.execute(httpPost);
                inputStream = httpResponse.getEntity().getContent();

                if (inputStream != null)
                    result = convertInputStreamToString(inputStream);
                else
                    result = "Did not work!";
            } catch (Exception e) {
                Log.d(TAG, e.getLocalizedMessage());
            }
            return result;
        } else
            return STATUS_CODE_001;
    }

    public void setPostExecuteListener(PostExecuteListener postExecuteListener) {
        mPostExecuteListener = postExecuteListener;
    }

    public interface PostExecuteListener {
        void onPostExecute(String result);
    }

    private static String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while ((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;

    }

}
