package com.pos.empressa.nexgo_pos.Nexgo;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.nexgo.oaf.apiv3.APIProxy;
import com.nexgo.oaf.apiv3.DeviceEngine;
import com.nexgo.oaf.apiv3.device.pinpad.DukptKeyTypeEnum;
import com.nexgo.oaf.apiv3.device.pinpad.PinPad;

import io.flutter.app.FlutterApplication;
import io.flutter.plugin.common.MethodChannel;

public class NexgoApplication extends FlutterApplication {

    private Context mContext;

    private String IPEK = "3F2216D8297BCE9C";

    public NexgoApplication(Context mContext) {
        this.mContext = mContext;
    }

    public void initEmv(MethodChannel.Result result) {
        DeviceEngine deviceEngine = APIProxy.getDeviceEngine(mContext);

        int INJECTED_PIN_SLOT = 0;

        byte[] iPekByte = hexToByteArr(IPEK);

        PinPad pinPad = deviceEngine.getPinPad();
        if (pinPad.dukptCurrentKsn(INJECTED_PIN_SLOT) != null) {
            int i = pinPad.dukptKeyInject(0, DukptKeyTypeEnum.IPEK, iPekByte, iPekByte.length, hexToByteArr(getInitialKSN()));
            new Handler(Looper.getMainLooper()).post(() -> {
                if (i == 0) {
                    Toast.makeText(mContext, "key init success " + i, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, "key init failed " + i, Toast.LENGTH_SHORT).show();
                }
                result.success(i);
            });
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
