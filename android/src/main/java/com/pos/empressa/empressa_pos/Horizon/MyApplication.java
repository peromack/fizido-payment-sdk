package com.pos.empressa.empressa_pos.Horizon;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.horizonpay.smartpossdk.PosAidlDeviceServiceUtil;
import com.horizonpay.smartpossdk.aidl.IAidlDevice;
import com.horizonpay.utils.BaseUtils;

import io.flutter.app.FlutterApplication;

public class MyApplication extends FlutterApplication {
    private static final String TAG = "MyApplication";
    private static MyApplication INSTANCE;
    private IAidlDevice device;
    public Context mContext;


    public MyApplication(Context mContext) {
        this.mContext = mContext;
    }

    public static MyApplication getINSTANCE(){
        return INSTANCE;
    }
    public IAidlDevice getDevice(){
        return device;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
        BaseUtils.init(this);
//        AppLog.debug(true);
        bindDriverService();
    }

    public void initializeHPos(Context context) {
        bindDriverService();
    }


    public void bindDriverService() {
        Log.d("context", this.toString());
        PosAidlDeviceServiceUtil.connectDeviceService(mContext, new PosAidlDeviceServiceUtil.DeviceServiceListen() {
            @Override
            public void onConnected(IAidlDevice device) {
                MyApplication.this.device = device;
                try {
                    DeviceHelper.reset();
                    DeviceHelper.initDevices(MyApplication.this);
                    MyApplication.this.device.asBinder().linkToDeath(deathRecipient, 0);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void error(int errorcode) {
            }

            @Override
            public void onDisconnected() {

            }

            @Override
            public void onUnCompatibleDevice() {
            }
        });
    }

    private IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {

            if (MyApplication.this.device == null) {
//                AppLog.d(TAG,"binderDied device is null");
                return;
            }

            MyApplication.this.device.asBinder().unlinkToDeath(deathRecipient, 0);
            MyApplication.this.device = null;

            //reBind driver Service
            bindDriverService();
        }
    };
}
