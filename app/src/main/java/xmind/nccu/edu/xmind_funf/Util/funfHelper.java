package xmind.nccu.edu.xmind_funf.Util;

import android.text.format.DateFormat;

import java.util.Calendar;
import java.util.Locale;

/**
 * Created by sid.ku on 7/30/15.
 */
public class FunfHelper {

    /**
     * Convert timestamp from 'System.currentTimeMillis()'
     * */
    public static String getDate(String timestamp) {
        try{
            long time = Long.parseLong(timestamp);
            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
            cal.setTimeInMillis(time);
            String date = DateFormat.format("dd/MM/yyyy hh:mm a", cal).toString();
            return date;
        }catch(Exception e){
            return "";
        }
    }

}
