package com.pos.empressa.empressa_pos;

import androidx.annotation.NonNull;


import android.app.Activity;
import android.content.Context;


import com.pos.empressa.empressa_pos.MPos.MPosDeviceConnect;
import com.pos.empressa.empressa_pos.MPos.MPosApplication;
import com.pos.empressa.empressa_pos.Sunyard.SunyardApplication;
import com.pos.empressa.empressa_pos.Sunyard.SunyardPrinter;
import com.pos.empressa.empressa_pos.Sunyard.SunyardReadCard;
import com.socsi.utils.Log;

import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/**
 * EmpressaPosPlugin
 */
public class EmpressaPosPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private MethodChannel channel;
    SunyardReadCard sunyardReadCard;
    private Context mContext;
    MPosApplication mPosApplication ;
    MPosDeviceConnect mPosDeviceConnect;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "empressa_pos");
        channel.setMethodCallHandler(this);

    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        sunyardReadCard = new SunyardReadCard(mContext);


        switch (call.method) {
            case "searchCard":
                sunyardReadCard.searchCard(result, call.argument("transactionAmount"));
                break;
            case "stopSearch":
                sunyardReadCard.stopSearch();
                break;
            case "initEmv":
                SunyardApplication sunyardApplication = new SunyardApplication();
                sunyardApplication.initializeApp(mContext);
                break;
            case "startPrinter":
                SunyardPrinter sunyardPrinter = new SunyardPrinter(mContext);
                Log.d("PrintActivity.class", call.arguments.toString());
                sunyardPrinter.startPrint(call);
                break;
            case "checkSunyardCard":
                sunyardReadCard.checkCard(result);
                break;
                case "initializeMPos":
                  mPosApplication.initializeMPos(mContext) ;
                break;
            case "connectMPos":
                mPosDeviceConnect.connectDevice(call.argument("bluetoothName"), call.argument("bluetoothMac"),result,mContext);
                break;
            case "startMPosDiscovery":
                mPosDeviceConnect.startDiscovery(result);
                break;
            case "removeMPosBondMethods":
                mPosDeviceConnect.removeBondMethods();
                break;
            case "unregisterMPosReceiver":
                mPosDeviceConnect.unregisterReceiver();
                break;
            case "registerMPosReceiver":
                mPosDeviceConnect.registerReceiver();
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }


    @Override
    public void onAttachedToActivity(@NonNull @org.jetbrains.annotations.NotNull ActivityPluginBinding binding) {
        // TODO: your plugin is now attached to an Activity
        Activity activity = binding.getActivity();
        mContext = binding.getActivity().getApplicationContext();
        mPosApplication = new  MPosApplication();
        mPosApplication.initializeMPos(mContext) ;
        mPosDeviceConnect = new MPosDeviceConnect(activity);


    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        // TODO: the Activity your plugin was attached to was destroyed to change configuration.
        // This call will be followed by onReattachedToActivityForConfigChanges().
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull @org.jetbrains.annotations.NotNull ActivityPluginBinding binding) {
        // TODO: your plugin is now attached to a new Activity after a configuration change.

    }

    @Override
    public void onDetachedFromActivity() {
        sunyardReadCard.stopSearch();
        // TODO: your plugin is no longer associated with an Activity. Clean up references.

    }

}
