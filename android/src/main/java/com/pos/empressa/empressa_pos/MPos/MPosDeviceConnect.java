package com.pos.empressa.empressa_pos.MPos;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;


import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.mf.mpos.pub.Controler;
import com.mf.mpos.pub.result.ConnectPosResult;
import com.pos.empressa.empressa_pos.MPos.model.BluetoothItem;
import com.pos.empressa.empressa_pos.MPos.utils.SweetDialogUtils;
import com.pos.empressa.empressa_pos.R;


import java.lang.reflect.Method;

import java.util.Set;
import java.util.*;

import cn.pedant.SweetAlert.SweetAlertDialog;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.plugin.common.MethodChannel;

public class MPosDeviceConnect extends FlutterActivity {

    private final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
   // private BluetoothListAdapter mAdapter;
    private BluetoothReceiver br;
    private String bluetoothMac = "";
    private String bluetoothName = "";
    private final ArrayList<BluetoothItem> bluetoothItems =  new  ArrayList<BluetoothItem>();
    Activity activity ;

    public MPosDeviceConnect(Activity activity){
        btAdapter.enable();
//        if (!MPosApplication.getBluetoothMac().isEmpty()) {
////            ll_bluetoothDevice.setVisibility(View.VISIBLE);
////            tv_name.setText(MyApplication.getBluetoothName());
////            tv_mac.setText(MyApplication.getBluetoothMac());
//        } else {
//           // ll_bluetoothDevice.setVisibility(View.GONE);
//        }
        this.activity = activity ;
        registerReceiver();
    }




    public void unregisterReceiver() {
        if (br != null) {
            try {
                unregisterReceiver(br);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        br = null;
    }

    public void registerReceiver() {
        try {
            br = new BluetoothReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            registerReceiver(br, filter);
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    private class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            try {
                if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                    BluetoothDevice btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (btDevice != null) {
                        bluetoothItems.add(new BluetoothItem(btDevice.getName(), btDevice.getAddress(), false));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void startDiscovery(@NonNull MethodChannel.Result result) {
        btAdapter.cancelDiscovery();
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        bluetoothItems.clear();
        for (BluetoothDevice device : pairedDevices) {
            bluetoothItems.add(new BluetoothItem(device.getName(), device.getAddress(), false));
        }
        Gson gson = new Gson();
        String json = gson.toJson(bluetoothItems);
        Log.d("Final DEVICES LIST", json);
        result.success(json);
        btAdapter.startDiscovery();
    }

    public void connectDevice(String bleName,String bleMac,@NonNull MethodChannel.Result result,Context context) {
        btAdapter.cancelDiscovery();
        //SweetDialogUtils.showProgress(MPosDeviceConnect.this, "Device Connect", false);
        if (bleMac != null ) {
            BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothManager.getAdapter().cancelDiscovery();
            bluetoothMac = bleMac;
            bluetoothName = bleName;
        }else{
            bluetoothMac = MPosApplication.getBluetoothMac();
            bluetoothName = MPosApplication.getBluetoothName();
        }

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (Controler.posConnected()) {
                    Controler.disconnectPos();
                }
                ConnectPosResult ret = Controler.connectPos(bluetoothMac);

                if (ret.bConnected) {
                    MPosApplication.setBluetoothMac(bluetoothMac);
                    MPosApplication.setBluetoothName(bluetoothName);
                    result.success(true);
                    // SweetDialogUtils.changeAlertType(MPosDeviceConnect.this, "Device already connect", SweetAlertDialog.SUCCESS_TYPE);


                } else {
                    result.success(false);
                    //SweetDialogUtils.changeAlertType(MPosDeviceConnect.this, ">Device connect fail", SweetAlertDialog.ERROR_TYPE);
                }
            }
        });

    }

    public void removeBondMethods() {
        Controler.disconnectPos();
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        for (BluetoothDevice b : pairedDevices) {
            removeBondMethod(b);
        }
//        mAdapter.clear();
//        mAdapter.notifyDataSetChanged();
    }

    private int removeBondMethod(BluetoothDevice btDev) {
        // TODO Auto-generated method stub
        //Using reflection method calls BluetoothDevice.createBond(BluetoothDevice remoteDevice);
        Method removeBondMethod ;
        try {
            removeBondMethod = BluetoothDevice.class.getMethod("removeBond");
            removeBondMethod.invoke(btDev);
            Log.w("removeBondMethod", "removeBondMethod  	removeBondMethod.invoke(btDev); ");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return 0;
    }


}
