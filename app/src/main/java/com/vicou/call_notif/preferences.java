package com.vicou.call_notif;

import android.content.Context;
import android.content.SharedPreferences;

public class preferences {
    Context _context;
    SharedPreferences _preferences;
    public preferences(Context context){
        _context=context;
        _preferences=_context.getSharedPreferences("techlist",Context.MODE_PRIVATE);
    }

    public void setFirstStart(boolean firstStart){_preferences.edit().putBoolean("firstStart",firstStart).apply();}
    public boolean getFirstStart(){return _preferences.getBoolean("firstStart",false);}

    public void setToken(String token){
        _preferences.edit().putString("token",token).apply();
    }
    public String getToken(){
        return _preferences.getString("token",null);
    }

    public void setUrl(String url){
        _preferences.edit().putString("url",url).apply();
    }
    public String getUrl(){
        return _preferences.getString("url",null);
    }

    public void setWifi(boolean wifi){
        _preferences.edit().putBoolean("wifi",wifi).apply();
    }
    public boolean getWifi(){
        return _preferences.getBoolean("wifi",false);
    }

    public void setWifiName(String wifiName){_preferences.edit().putString("wifi_name",wifiName).apply();}
    public String getWifiName(){
        return _preferences.getString("wifi_name",null);
    }

    public void setDevice(String device){
        _preferences.edit().putString("device",device).apply();
    }
    public String getDevice(){
        return _preferences.getString("device",null);
    }
}
