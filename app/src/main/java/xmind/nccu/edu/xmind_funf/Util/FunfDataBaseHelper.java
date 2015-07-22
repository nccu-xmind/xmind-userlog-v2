package xmind.nccu.edu.xmind_funf.Util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

import edu.mit.media.funf.storage.NameValueDatabaseHelper;
import edu.mit.media.funf.time.TimeUtil;
import edu.mit.media.funf.util.UuidUtil;

/**
 * Created by sid.ku on 7/15/15.
 */
public class FunfDataBaseHelper extends SQLiteOpenHelper {
    private Context mContext;
    public static final int CURRENT_VERSION = 1;
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_VALUE = "value";
    public static final NameValueDatabaseHelper.Table DATA_TABLE = new NameValueDatabaseHelper.Table("data", Arrays.asList(new NameValueDatabaseHelper.Column[]{new NameValueDatabaseHelper.Column("name", "TEXT"), new NameValueDatabaseHelper.Column("timestamp", "FLOAT"), new NameValueDatabaseHelper.Column("value", "TEXT")}));
    public static final String COLUMN_DATABASE_NAME = "dbname";
    public static final String COLUMN_INSTALLATION = "device";
    public static final String COLUMN_UUID = "uuid";
    public static final String COLUMN_CREATED = "created";
    public static final NameValueDatabaseHelper.Table FILE_INFO_TABLE = new NameValueDatabaseHelper.Table("file_info", Arrays.asList(new NameValueDatabaseHelper.Column[]{new NameValueDatabaseHelper.Column("dbname", "TEXT"), new NameValueDatabaseHelper.Column("device", "TEXT"), new NameValueDatabaseHelper.Column("uuid", "TEXT"), new NameValueDatabaseHelper.Column("created", "FLOAT")}));
    private final String databaseName;

    public FunfDataBaseHelper(Context context, String name) {
//        super(context, name, (SQLiteDatabase.CursorFactory)null, version);
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
        db.execSQL(String.format(Locale.US, "insert into %s (%s, %s, %s, %s) values (\'%s\', \'%s\', \'%s\', %f)", new Object[]{FILE_INFO_TABLE.name, "dbname", "device", "uuid", "created", this.databaseName, installationUuid, fileUuid, Double.valueOf(createdTime)}));
    }

    //Testing...
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = "DROP TABLE IF EXISTS " + databaseName;
        db.execSQL(sql);
        onCreate(db);
    }

    /* select all record */
    public Cursor selectData(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("data", new String[] {"_id", "name", "timestamp", "value"}, null, null, null, null, null);
        return cursor;
    }

    /* select all record2 */
    public Cursor selectData2(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("file_info", new String[] {"_id", "dbname", "device", "uuid", "created"}, null, null, null, null, null);
        return cursor;
    }

    //testing for add wifi probes
    public long addLog(String name, String value, String timestamp){
        //testing - insert testing data.
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("value", value);
        cv.put("timestamp", Double.valueOf(timestamp));
        long row = db.insert("data", null, cv);
        return row;
    }

    public Cursor selectDB(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("data", new String[] {"_id", "name", "timestamp", "value"}, null, null, null, null, null);
//        if(cursor.getCount() > 0){
//            cursor.moveToFirst();
//            for(int i = 0; i < cursor.getCount(); i++){
//                Log.v("ssku", "====Column " + i + " ID : " + cursor.getString(0) + "Column 1 : " + cursor.getString(1) + ", Column 2 : " + cursor.getString(2) + ", Column 3 : " + cursor.getString(3));
//                cursor.moveToNext();
//            }
//        }else
//            Log.e("ssku", "selectDB couldn't get any data...");
        return cursor;
    }
}
