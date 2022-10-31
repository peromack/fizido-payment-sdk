package com.pos.empressa.empressa_pos.Nexgo;

import android.content.Context;

import com.nexgo.oaf.apiv3.APIProxy;

import io.flutter.app.FlutterApplication;

public class NexgoApplication extends FlutterApplication {

    private Context mContext;

    public NexgoApplication(Context mContext) {
        this.mContext = mContext;
    }

    public void initEmv() {
        APIProxy.getDeviceEngine(mContext);
    }
}
