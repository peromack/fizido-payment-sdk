package com.pos.empressa.nexgo_pos.Nexgo;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.nexgo.oaf.apiv3.APIProxy;
import com.nexgo.oaf.apiv3.DeviceEngine;
import com.nexgo.oaf.apiv3.device.pinpad.DukptKeyTypeEnum;

import io.flutter.app.FlutterApplication;

public class NexgoApplication extends FlutterApplication {

    private Context mContext;

    private String IPEK = "3F2216D8297BCE9C";

    public NexgoApplication(Context mContext) {
        this.mContext = mContext;
    }

    public void initEmv() {
        DeviceEngine deviceEngine = APIProxy.getDeviceEngine(mContext);

        byte[] iPekByte = hexToByteArr(IPEK);

        int result = deviceEngine.getPinPad().dukptKeyInject(0, DukptKeyTypeEnum.IPEK, iPekByte, iPekByte.length, hexToByteArr(getInitialKSN()));
        if (result == 0) {
            Toast.makeText(mContext, "key init success " + result, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(mContext, "key init failed " + result, Toast.LENGTH_SHORT).show();
        }
    }

    private byte[] hexToByteArr(String hexString) {
        if(hexString == null) return null;
        byte[] byteArray = new byte[hexString.length() / 2];
        for (int i = 0; i < byteArray.length; i++) {
            int index = i * 2;
            int j = Integer.parseInt(hexString.substring(index, index + 2), 16);
            byteArray[i] = (byte) j;
        }

        return byteArray;
    }

    private String getInitialKSN(){
        SharedPreferences sharedPref = mContext.getSharedPreferences( "KSNCOUNTER", Context.MODE_PRIVATE);
        int ksn = sharedPref.getInt("KSN",00001);
        if(ksn > 9999){
            ksn = 00000 ;
        }
        int latestKSN = ksn + 1 ;
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("KSN",latestKSN);
        editor.apply();
        return  "0000000002DDDDE"+ String.format("%05d", latestKSN);
    }
}
