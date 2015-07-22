package xmind.nccu.edu.xmind_funf.Util;

import java.io.Serializable;

/**
 * Created by sid.ku on 7/21/15.
 */
public class ProbesObject implements Serializable {
    private static final long serialVersionUID = 1L;
    private String probeName = "";
    private String value = "";
    private String timestamp = "";

    public ProbesObject(String probeName, String value, String timestamp){
        this.probeName = probeName;
        this.value = value;
        this.timestamp = timestamp;
    }

    public String getProbeName(){
        return probeName;
    }

    public String getValue(){
        return value;
    }

    public String getTimestamp(){
        return timestamp;
    }

}
