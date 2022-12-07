package com.pos.empressa.horizon_pos.MPos;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;


import io.flutter.app.FlutterApplication;

public class MPosApplication extends FlutterApplication {

    private static MPosApplication mInstance;

    public static MPosApplication getInstance() {
        return mInstance;
    }

    private static SharedPreferences mPerferences;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        //initApplication();
    }

    public void initializeMPos(Context context) {
        initApplication(context);
    }

    public void initApplication(Context context) {
        mPerferences = PreferenceManager.getDefaultSharedPreferences(context);
    }


    public static void setManufacturerID(int id) {
        SharedPreferences.Editor mEditor = mPerferences.edit();
        mEditor.putInt("manufacturerID", id);
        mEditor.commit();
    }

    public static int getManufacturerID() {
        return mPerferences.getInt("manufacturerID", 0);
    }

    public static void setConnectedMode(String connectedMode) {
        SharedPreferences.Editor mEditor = mPerferences.edit();
        mEditor.putString("connectedMode", connectedMode);
        mEditor.commit();
    }

    public static String getConnectedMode() {
        return mPerferences.getString("connectedMode", "Bluetooth");
    }

    public static void setBluetoothMac(String mac) {
        SharedPreferences.Editor mEditor = mPerferences.edit();
        mEditor.putString("BluetoothMac", mac);
        mEditor.commit();
    }

    public static String getBluetoothMac() {
        return mPerferences.getString("BluetoothMac", "");
    }

    public static void setBluetoothName(String name) {
        SharedPreferences.Editor mEditor = mPerferences.edit();
        mEditor.putString("BluetoothName", name);
        mEditor.commit();
    }

    public static String getBluetoothName() {
        return mPerferences.getString("BluetoothName", "");
    }
    
    

}
