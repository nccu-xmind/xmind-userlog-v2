package xmind.nccu.edu.xmind_funf.Util;

import java.io.Serializable;

/**
 * Created by sid.ku on 7/21/15.
 */
public class ProbesObject implements Serializable {
    private static final long serialVersionUID = 1L;
    private String probeName = "";
    private String timestamp = "";
    private String batteryLevel = "";
    private String process = "";
    private String latitude = "";
    private String longitude = "";
    private String rssi = "";
    private String isScreenOn = "";
    private String packageName = "";
    private String wifitag = "";
    private String mobileData = "";
    private String model = "";
    private String deviceId = "";

    public ProbesObject(){}

    public void setProbeName(String probeName){
        this.probeName = probeName;
    }
    public String getProbeName(){
        return probeName;
    }

    public void setTimestamp(String timestamp){
        this.timestamp = timestamp;
    }
    public String getTimestamp(){
        return timestamp;
    }

    public void setBatteryLevel(String batteryLevel){
        this.batteryLevel = batteryLevel;
    }
    public String getBatteryLevel(){
        return batteryLevel;
    }

    public void setProcess(String process){
        this.process = process;
    }
    public String getProcess(){
        return process;
    }

    public void setLatitude(String latitude){
        this.latitude = latitude;
    }
    public String getLatitude(){
        return latitude;
    }

    public void setLongitude(String longitude){
        this.longitude = longitude;
    }
    public String getLongitude(){
        return longitude;
    }

    public void setRSSI(String rssi){
        this.rssi = rssi;
    }
    public String getRSSI(){
        return rssi;
    }

    public void setIsScreenOn(String isScreenOn){
        this.isScreenOn = isScreenOn;
    }
    public String getIsScreenOn(){
        return isScreenOn;
    }

    public void setPackageName(String packageName){
        this.packageName = packageName;
    }
    public String getPackageName(){
        return packageName;
    }

    public void setWifitag(String wifitag){
        this.wifitag = wifitag;
    }
    public String getWifitag(){
        return wifitag;
    }

    public void setMobileData(String mobileData){
        this.mobileData = mobileData;
    }
    public String getMobileData(){
        return mobileData;
    }

    public void setModel(String model){
        this.model = model;
    }
    public String getModel(){
        return model;
    }

    public void setDeviceId(String deviceId){
        this.deviceId = deviceId;
    }
    public String getDeviceId(){
        return deviceId;
    }

}
