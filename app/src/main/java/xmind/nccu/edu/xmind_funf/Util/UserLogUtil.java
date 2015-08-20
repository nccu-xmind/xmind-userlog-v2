package xmind.nccu.edu.xmind_funf.Util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by sid.ku on 8/20/15.
 */
public class UserLogUtil {
    public static final String funf_xmind_SP = "funf_xmind_SP";

    public static final String getCallLog = "funf_xmind_CallLog";
    public static final String getHardwareInfo = "funf_xmind_HardwareInfo";

    static SharedPreferences funf_xmind_sp;

    public static SharedPreferences GetSharedPreferencesForTimeControl(Context mCtx){
        funf_xmind_sp = mCtx.getSharedPreferences(funf_xmind_SP, Context.MODE_PRIVATE);
        return funf_xmind_sp;
    }
}
