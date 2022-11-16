package com.pos.empressa.nexgo_pos.Horizon;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.horizonpay.smartpossdk.PosAidlDeviceServiceUtil;
import com.horizonpay.smartpossdk.aidl.IAidlDevice;
import com.horizonpay.smartpossdk.aidl.emv.AidEntity;
import com.horizonpay.smartpossdk.aidl.emv.CapkEntity;
import com.horizonpay.smartpossdk.aidl.emv.IAidlEmvL2;
import com.horizonpay.utils.BaseUtils;
import com.pos.empressa.nexgo_pos.Horizon.utils.AidsUtil;

import java.util.List;

import io.flutter.app.FlutterApplication;

public class MyApplication extends FlutterApplication {
    private static final String TAG = "MyApplication";
    private static MyApplication INSTANCE;
    private IAidlDevice device;
    private IAidlEmvL2 mEmvL2;
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
//        initEmv(mContext);
        Log.d("creating", "horizon application class");
        bindDriverService();
    }

    public void initializeHPos(Context context) {
        bindDriverService();
    }

    public void initEmv() {
        try {
            Log.d("initializing emv", "hhhh");
            mEmvL2 = DeviceHelper.getEmvHandler();
            boolean ret = mEmvL2.deleteAllAids();
            if (!ret) {
                Log.d("AID", "remove aid failed");
            }
            downloadAID();

            boolean ret2 = mEmvL2.deleteAllCapks();
            if(!ret2){
                Log.d("CAPK", "remove capk failed");
            }
            downloadCAPK();
        } catch (RemoteException e) {
            e.printStackTrace();
//            ToastUtils.showShort(e.getMessage());
        }
    }

    private void downloadAID() {
        List<AidEntity> aidEntityList = AidsUtil.getAllAids();
        boolean ret = false;
        for (int i = 0; i < aidEntityList.size(); i++) {
            String tip = "Download aid" + String.format("(%d)", i);
            AidEntity emvAidPara = aidEntityList.get(i);
            try {
                ret = mEmvL2.addAid(emvAidPara);
                if (!ret) {
                    Log.d("AID", "add aid failed");
                    break;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
//            SystemClock.sleep(50);
//            showResult(textView, "Download aid :" + emvAidPara.getAID() + (ret == true ? " success" : " fail"));
        }
        Log.d("AID ", "add aid success");
    }

    private void downloadCAPK() {
        List<CapkEntity> capkEntityList = AidsUtil.getAllCapks();
        try {
            mEmvL2.addCapks(capkEntityList);
            Log.d("CApk ", "add cap k success");
        } catch (RemoteException e) {
            e.printStackTrace();
        }


        boolean ret = false;
        for (int i = 0; i < capkEntityList.size(); i++) {
            String tip = "Download capk" + String.format("(%d)", i);
            CapkEntity emvCapkPara = capkEntityList.get(i);
            try {
                ret = mEmvL2.addCapk(emvCapkPara);
                if (!ret) {
                    Log.d("CAPK", "add capk failed");
                    break;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
//            SystemClock.sleep(50);
        }
    }

    private void clearCAPK() {
        try {
            boolean ret = mEmvL2.deleteAllCapks();
            if (ret) {
            } else {
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
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
                    initEmv();
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
