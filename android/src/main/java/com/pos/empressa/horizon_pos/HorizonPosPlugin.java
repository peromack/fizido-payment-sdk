package com.pos.empressa.horizon_pos;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;


import android.app.Activity;
import android.content.Context;
import android.os.Build;


import com.pos.empressa.horizon_pos.Blusalt.BlusaltApiService;
import com.pos.empressa.horizon_pos.Fizido.FizidoApiService;
import com.pos.empressa.horizon_pos.Horizon.DeviceHelper;
import com.pos.empressa.horizon_pos.Horizon.HorizonPrinter;
import com.pos.empressa.horizon_pos.Horizon.HorizonReadCard;
import com.pos.empressa.horizon_pos.Horizon.MyApplication;
import com.socsi.utils.Log;

import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/**
 * HorizonPosPlugin
 */
public class HorizonPosPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private MethodChannel channel;
    HorizonReadCard horizonReadCard;
    private Context mContext;

    MyApplication hPosApplication;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "horizon_pos");
        channel.setMethodCallHandler(this);

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        horizonReadCard = new HorizonReadCard(mContext);


        switch (call.method) {
            case "stopHorizonSearch":
                horizonReadCard.stopEmvProcess();
                break;
            case "initHorizonEmv":
                MyApplication myApplication = new MyApplication(mContext);
                myApplication.bindDriverService();
                break;
            case "startHorizonPrinter":
                HorizonPrinter horizonPrinter = new HorizonPrinter(mContext);
                Log.d("PrintActivity.class", call.arguments.toString());
                horizonPrinter.print(call);
                break;
            case "chargeBlusaltTransaction":
                BlusaltApiService.chargeTransaction(result, mContext, call);
                break;
            case "chargeFidizoTransaction":
                FizidoApiService.chargeFidizoTransaction(result, mContext, call);
                break;
            case "horizonSearchCard":
                horizonReadCard.searchCard(result, call.argument("transactionAmount"));
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

        //Horizon application init
        hPosApplication = new MyApplication(mContext);
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
        horizonReadCard.stopEmvProcess();
        // TODO: your plugin is no longer associated with an Activity. Clean up references.

    }

}
