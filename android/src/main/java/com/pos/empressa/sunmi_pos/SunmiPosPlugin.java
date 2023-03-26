package com.pos.empressa.sunmi_pos;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;


import android.app.Activity;
import android.content.Context;
import android.os.Build;


import com.pos.empressa.sunmi_pos.Blusalt.BlusaltApiService;
import com.pos.empressa.sunmi_pos.Fizido.FizidoApiService;
import com.pos.empressa.sunmi_pos.Nexgo.NexgoApplication;
import com.pos.empressa.sunmi_pos.Nexgo.NexgoPrinter;
import com.pos.empressa.sunmi_pos.Nexgo.NexgoReadCard;
import com.pos.empressa.sunmi_pos.Sunmi.SunmiApplication;
import com.pos.empressa.sunmi_pos.Sunmi.SunmiPrinter;
import com.pos.empressa.sunmi_pos.Sunmi.SunmiReadCard;

import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/**
 * SunmiPosPlugin
 */
public class SunmiPosPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private MethodChannel channel;
    SunmiReadCard sunmiReadCard;
    private Context mContext;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "sunmi_pos");
        channel.setMethodCallHandler(this);

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        sunmiReadCard = new SunmiReadCard(mContext);

        switch (call.method) {
            case "initSunmiEmv":
                SunmiApplication sunmiApplication = new SunmiApplication(mContext);
                sunmiApplication.setupTerminal();
                break;
            case "startSunmiPrinter":
                SunmiPrinter sunmiPrinter = new SunmiPrinter(mContext);
                sunmiPrinter.sunmiPrint(call);
                break;
            case "chargeBlusaltTransaction":
                BlusaltApiService.chargeTransaction(result, mContext, call);
                break;
            case "chargeFidizoTransaction":
                FizidoApiService.chargeFidizoTransaction(result, mContext, call);
                break;
            case "sunmiSearchCard":
                sunmiReadCard.searchCard(result, call.argument("transactionAmount"));
                break;
            case "cancelSunmiSearch":
                sunmiReadCard.cancelCheckCard();
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
        sunmiReadCard.cancelCheckCard();
        // TODO: your plugin is no longer associated with an Activity. Clean up references.

    }

}
