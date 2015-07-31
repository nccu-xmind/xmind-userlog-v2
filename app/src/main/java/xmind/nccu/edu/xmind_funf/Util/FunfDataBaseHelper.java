package xmind.nccu.edu.xmind_funf.Util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Arrays;
import java.util.UUID;

import edu.mit.media.funf.storage.NameValueDatabaseHelper;
import edu.mit.media.funf.time.TimeUtil;
import edu.mit.media.funf.util.UuidUtil;

/**
 * Created by sid.ku on 7/15/15.
 */
public class FunfDataBaseHelper extends SQLiteOpenHelper {
    private Context mContext;
    public static final String XMIND_FUNF_DATABASE_NAME = "XmindFunf_Database";
    public static final String XMIND_FUNF_DATABASE_DEVICE = "Device_info";
    public static final int CURRENT_VERSION = 1;
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_VALUE = "value";
    public static final NameValueDatabaseHelper.Table DATA_TABLE = new NameValueDatabaseHelper.Table(XMIND_FUNF_DATABASE_NAME, Arrays.asList(
            new NameValueDatabaseHelper.Column[]{new NameValueDatabaseHelper.Column("name", "TEXT"),
                    new NameValueDatabaseHelper.Column("timestamp", "TEXT"),
                    new NameValueDatabaseHelper.Column("batteryLevel", "FLOAT"),
                    new NameValueDatabaseHelper.Column("process", "TEXT"),
                    new NameValueDatabaseHelper.Column("latitude", "FLOAT"),
                    new NameValueDatabaseHelper.Column("longitude", "FLOAT"),
                    new NameValueDatabaseHelper.Column("rssi", "FLOAT"),
                    new NameValueDatabaseHelper.Column("isScreenOn", "TEXT"),
                    new NameValueDatabaseHelper.Column("packageName", "TEXT"),
                    new NameValueDatabaseHelper.Column("wifitag", "TEXT"),
                    new NameValueDatabaseHelper.Column("mobiletag", "TEXT")}));
    public static final String COLUMN_DATABASE_NAME = "dbname";
    public static final String COLUMN_INSTALLATION = "device";
    public static final String COLUMN_UUID = "uuid";
    public static final String COLUMN_CREATED = "created";
    public static final NameValueDatabaseHelper.Table FILE_INFO_TABLE = new NameValueDatabaseHelper.Table(XMIND_FUNF_DATABASE_DEVICE, Arrays.asList(
            new NameValueDatabaseHelper.Column[]{new NameValueDatabaseHelper.Column("name", "TEXT"),
                    new NameValueDatabaseHelper.Column("timestamp", "TEXT"),
                    new NameValueDatabaseHelper.Column("model", "TEXT"),
                    new NameValueDatabaseHelper.Column("deviceId", "TEXT")}));

    //Columns name
    public static final String CURRENT_FOREGROUND_APP = "Current_ForeGround_AppName";
    public static final String CURRENT_FOREGROUND_APP_ON_NEW_PICUTR = "Current_ForeGround_Camera_AppName";
    public static final String CURRENT_FOREGROUND_APP_AFTER_SCREEN_UNLICK = "Current_ForeGround_Screen_Unlock_AppName";
    public static final String TAKE_A_NEW_PHOTO_EVENT = "Take_a_New_Photo_Event";

    //TAG
    public static final String REGULAR_CHECK = "Regular_Check_fg_App";
    public static final String WIFI_STATUS_PROBE = "Wifi_Status";

    private final String databaseName;

    public FunfDataBaseHelper(Context context, String name) {
        super(context, COLUMN_DATABASE_NAME, null, CURRENT_VERSION);
        this.mContext = context;
        this.databaseName = name;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATA_TABLE.getCreateTableSQL());
        db.execSQL(FILE_INFO_TABLE.getCreateTableSQL());
        String installationUuid = UuidUtil.getInstallationId(this.mContext);
        String fileUuid = UUID.randomUUID().toString();
        double createdTime = TimeUtil.getTimestamp().doubleValue();
//        db.execSQL(String.format(Locale.US, "insert into %s (%s, %s, %s, %s) values (\'%s\', \'%s\', \'%s\', %f)", new Object[]{FILE_INFO_TABLE.name, "name", "timestamp", "model", "deviceId", this.databaseName, installationUuid, fileUuid, Double.valueOf(createdTime)}));
    }

    //Testing...
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = "DROP TABLE IF EXISTS " + databaseName;
        db.execSQL(sql);
        onCreate(db);
    }

    /* select all record */
    public Cursor selectData() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(XMIND_FUNF_DATABASE_NAME, new String[]{"_id", "name", "timestamp", "value"}, null, null, null, null, null);
        return cursor;
    }

    /* select all record2 */
    public Cursor selectData2() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(XMIND_FUNF_DATABASE_DEVICE, new String[]{"_id", "dbname", "device", "uuid", "created"}, null, null, null, null, null);
        return cursor;
    }

    //testing for add wifi probes
//    public long addLog(String name, String value, String timestamp){
//        //testing - insert testing data.
//        SQLiteDatabase db = this.getWritableDatabase();
//        ContentValues cv = new ContentValues();
//        cv.put("name", name);
//        cv.put("value", value);
//        cv.put("timestamp", Double.valueOf(timestamp));
//        long row = db.insert(XMIND_FUNF_DATABASE_NAME, null, cv);
//        return row;
//    }

    public long addBatteryRecord(String name, String timestamp, String level) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("timestamp", timestamp);
        cv.put("batteryLevel", Float.valueOf(level));
        long row = db.insert(XMIND_FUNF_DATABASE_NAME, null, cv);
        return row;
    }

    public long addServiceRecord(String name, String timestamp, String process) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("timestamp", timestamp);
        cv.put("process", process);
        long row = db.insert(XMIND_FUNF_DATABASE_NAME, null, cv);
        return row;
    }

    public long addLocationRecord(String name, String timestamp, String mLatitude, String mLongitude) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("timestamp", timestamp);
        cv.put("latitude", Double.valueOf(mLatitude));
        cv.put("longitude", Double.valueOf(mLongitude));
        long row = db.insert(XMIND_FUNF_DATABASE_NAME, null, cv);
        return row;
    }

    public long addBluetoothRecord(String name, String timestamp, String RSSI) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("timestamp", timestamp);
        cv.put("rssi", Integer.valueOf(RSSI));
        long row = db.insert(XMIND_FUNF_DATABASE_NAME, null, cv);
        return row;
    }

    public long addScreenRecord(String name, String timestamp, String isScreenOn) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("timestamp", timestamp);
        cv.put("isScreenOn", isScreenOn);
        long row = db.insert(XMIND_FUNF_DATABASE_NAME, null, cv);
        return row;
    }

    public long addPhotoRecord(String name, String timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("timestamp", timestamp);
        long row = db.insert(XMIND_FUNF_DATABASE_NAME, null, cv);
        return row;
    }

    public long addCurrentForegroundAppRecord(String name, String timestamp, String packageName/*, String TAG*/) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("timestamp", timestamp);
        cv.put("packageName", packageName);
//        cv.put("tag", TAG);//1.First app, 2.After take photo, 3.Regular checking...etc
        long row = db.insert(XMIND_FUNF_DATABASE_NAME, null, cv);
        return row;
    }

    //Wifi Tag : 0 == wifi has been turned on and connected; 1 == wifi has been turned off; 2 == wifi turned on, but no signal currently.
    public long addNetworkStateRecord(String name, String timestamp, int wifiTag, boolean isMobileDataAvailable) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("timestamp", timestamp);
        cv.put("wifitag", wifiTag);
        cv.put("mobiletag", String.valueOf(isMobileDataAvailable));
        long row = db.insert(XMIND_FUNF_DATABASE_NAME, null, cv);
        return row;
    }

    public long addHardwareInfo(String name, String timestamp, String model, String deviceId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("timestamp", timestamp);
        cv.put("model", model);
        cv.put("deviceId", deviceId);
        long row = db.insert(XMIND_FUNF_DATABASE_DEVICE, null, cv);
        return row;
    }

    public Cursor selectDB() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(XMIND_FUNF_DATABASE_NAME, new String[]{"_id", "name", "timestamp", "batteryLevel", "process", "latitude", "longitude", "rssi", "isScreenOn", "packageName", "wifitag", "mobiletag"}, null, null, null, null, null);
        return cursor;
    }

    public Cursor selectDeviceDB() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(XMIND_FUNF_DATABASE_DEVICE, new String[]{"_id", "name", "timestamp", "model", "deviceId"}, null, null, null, null, null);
        return cursor;
    }
}
