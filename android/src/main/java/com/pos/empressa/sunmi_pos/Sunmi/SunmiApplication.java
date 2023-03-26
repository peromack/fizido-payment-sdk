package com.pos.empressa.sunmi_pos.Sunmi;

import android.content.Context;
import android.util.Log;

import com.pos.empressa.sunmi_pos.Sunmi.utils.EmvUtil;
import com.sunmi.pay.hardware.aidlv2.emv.EMVOptV2;
import com.sunmi.pay.hardware.aidlv2.pinpad.PinPadOptV2;
import com.sunmi.pay.hardware.aidlv2.readcard.ReadCardOptV2;
import com.sunmi.pay.hardware.aidlv2.security.SecurityOptV2;
import com.sunmi.pay.hardware.aidlv2.system.BasicOptV2;
import com.sunmi.peripheral.printer.InnerPrinterCallback;
import com.sunmi.peripheral.printer.InnerPrinterException;
import com.sunmi.peripheral.printer.InnerPrinterManager;
import com.sunmi.peripheral.printer.SunmiPrinterService;

import io.flutter.app.FlutterApplication;
import sunmi.paylib.SunmiPayKernel;

public class SunmiApplication extends FlutterApplication {

    public static SunmiApplication app;
    public PinPadOptV2 pinPadOptV2;
    public SecurityOptV2 securityOptV2;
    public BasicOptV2 basicOptV2;

    public SunmiPrinterService sunmiPrinterService;

    private Context mContext;

    public ReadCardOptV2 readCardOptV2;
    public EMVOptV2 emvOptV2;
    private boolean connectPaySDK;

    public SunmiApplication(Context mContext) {
        this.mContext = mContext;
        app = this;
    }

    public void setupTerminal() {
        bindPaySDKService();
        bindPrintService();
    }

    private void bindPaySDKService() {
        final SunmiPayKernel payKernel = SunmiPayKernel.getInstance();
        payKernel.initPaySDK(mContext, new SunmiPayKernel.ConnectCallback() {
            @Override
            public void onConnectPaySDK() {
                Log.d("SunmiApplication.TAG", "onConnectPaySDK...");
                emvOptV2 = payKernel.mEMVOptV2;
                readCardOptV2 = payKernel.mReadCardOptV2;
                pinPadOptV2 = payKernel.mPinPadOptV2;
                securityOptV2 = payKernel.mSecurityOptV2;
                basicOptV2 = payKernel.mBasicOptV2;
                connectPaySDK = true;
                EmvUtil.initAidAndRid();
            }

            @Override
            public void onDisconnectPaySDK() {
                Log.d("SunmiApplication.TAG", "onDisconnectPaySDK...");
                connectPaySDK = false;
                emvOptV2 = null;
                readCardOptV2 = null;
                pinPadOptV2 = null;
                securityOptV2 = null;
                basicOptV2 = null;
            }
        });
    }

    private void bindPrintService() {
        try {
            InnerPrinterManager.getInstance().bindService(mContext, new InnerPrinterCallback() {
                @Override
                protected void onConnected(SunmiPrinterService service) {
                    sunmiPrinterService = service;
                }

                @Override
                protected void onDisconnected() {
                    sunmiPrinterService = null;
                }
            });
        } catch (InnerPrinterException e) {
            e.printStackTrace();
        }
    }

}
