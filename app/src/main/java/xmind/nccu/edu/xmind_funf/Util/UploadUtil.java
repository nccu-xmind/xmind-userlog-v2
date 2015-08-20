package xmind.nccu.edu.xmind_funf.Util;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

/**
 * Created by sid.ku on 8/10/15.
 */
public class UploadUtil {
    public static final String HARDWARE_INFO_PROBE = "HardwareInfoProbe";
    public static final String WIFI_STATUS_PROBE = "Wifi_Status";
    public static final String LOCATION_PROBE = "LocationProbe";
    public static final String BLUETOOTH_PROBE = "BluetoothProbe";
    public static final String SCREEN_PROBE = "ScreenProbe";
    public static final String SERVICE_PROBE = "ServicesProbe";
    public static final String BATTERY_PROBE = "BatteryProbe";
    public static final String CALLLOG_PROBE = "CallLogProbe";
    //TODO
    //ProbeType，TriggeredTimestamp，Duration, Date
    public static final String TAKE_A_NEW_PHOTO_EVENT = "Take_a_New_Photo_Event";

    public static final String OBJ_MAIL = "UserID";
    public static final String OBJ_MODEL = "Model";
    public static final String OBJ_DEVICE = "HardwareID";
    public static final String OBJ_UPLOADING_TIME = "Timestamp";
    public static final String OBJ_PROBE_TYPE = "ProbeType";
    public static final String OBJ_TIMESTAMP = "TriggeredTimestamp";
    public static final String OBJ_NETWORK = "NatworkStatus";
    public static final String OBJ_LATITUDE = "Latitude";
    public static final String OBJ_LONGITUDE = "Longitude";
    public static final String OBJ_RSSI = "RSSI";
    public static final String OBJ_SCREEN = "ScreenOn";
    public static final String OBJ_PACKAGENAME = "PackageName";
    public static final String OBJ_PROCESS = "Process";
    public static final String OBJ_BATTERY = "BatteryLevel";
    public static final String OBJ_DURATION = "Duration";
    public static final String OBJ_DATE = "Date";
    public static final String PROBE_ARRAY = "ProbeArray";

    public static String getDeviceEmail(Context context) {
        AccountManager accountManager = AccountManager.get(context);
        Account account = getAccount(accountManager);
        if (account == null)
            return null;
        else
            return account.name;
    }

    private static Account getAccount(AccountManager accountManager) {
        Account[] accounts = accountManager.getAccountsByType("com.google");
        Account account;
        if (accounts.length > 0) {
            account = accounts[0];
        } else {
            account = null;
        }
        return account;
    }
}
