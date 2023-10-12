package com.pos.empressa.nexgo_pos;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;


import android.app.Activity;
import android.content.Context;
import android.os.Build;


import com.pos.empressa.nexgo_pos.Blusalt.BlusaltApiService;
import com.pos.empressa.nexgo_pos.Fizido.FizidoApiService;
import com.pos.empressa.nexgo_pos.Nexgo.NexgoApplication;
import com.pos.empressa.nexgo_pos.Nexgo.NexgoPrinter;
import com.pos.empressa.nexgo_pos.Nexgo.NexgoReadCard;

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
public class NexgoPosPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private MethodChannel channel;
    NexgoReadCard nexgoReadCard;
    private Context mContext;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "nexgo_pos");
        channel.setMethodCallHandler(this);

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        nexgoReadCard = new NexgoReadCard(mContext);

        switch (call.method) {
            case "initNexgoEmv":
                NexgoApplication nApplication = new NexgoApplication(mContext);
                nApplication.initEmv();
                break;
            case "startNexgoPrinter":
                NexgoPrinter nexgoPrinter = new NexgoPrinter(mContext);
                nexgoPrinter.nexgoPrint(call);
                break;
            case "chargeBlusaltTransaction":
                BlusaltApiService.chargeTransaction(result, mContext, call);
                break;
            case "chargeFidizoTransaction":
                FizidoApiService.chargeFidizoTransaction(result, mContext, call);
                break;
            case "nexgoSearchCard":
                nexgoReadCard.searchCard(result, call.argument("transactionAmount"));
                break;
            case "checkNexgoCard":
                nexgoReadCard.checkCard(result);
                break;
            case "cancelNexgoSearch":
                nexgoReadCard.cancelSearch();
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
        nexgoReadCard.cancelSearch();
        // TODO: your plugin is no longer associated with an Activity. Clean up references.

    }

}
