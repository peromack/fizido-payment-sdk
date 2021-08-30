package com.pos.empressa.empressa_pos;

import androidx.annotation.NonNull;


import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;



import com.socsi.aidl.pinservice.OperationPinListener;
import com.socsi.exception.PINPADException;
import com.socsi.exception.SDKException;
import com.socsi.smartposapi.card.CardReader;
import com.socsi.smartposapi.card.IcCardSearchCallback;
import com.socsi.smartposapi.card.MagCardSearchCallback;
import com.socsi.smartposapi.card.RfSearchCallback;
import com.socsi.smartposapi.card.rf.CardReaderConst;
import com.socsi.smartposapi.card.rf.RFSearchResultInfo;
import com.socsi.smartposapi.card.rf.UltralightDriver;
import com.socsi.smartposapi.emv2.AsyncEmvCallback;
import com.socsi.smartposapi.emv2.EmvL2;
import com.socsi.smartposapi.gmalgorithm.Dukpt;
import com.socsi.smartposapi.icc.Icc;
import com.socsi.smartposapi.magcard.CardInfo;
import com.socsi.smartposapi.magcard.Magcard;
import com.socsi.smartposapi.ped.Ped;
import com.socsi.smartposapi.terminal.TerminalManager;
import com.socsi.utils.DateUtil;
import com.socsi.utils.HexUtil;
import com.socsi.utils.Log;
import com.socsi.utils.StringUtil;
import com.socsi.utils.TlvUtil;
import com.pos.empressa.empressa_pos.R;
import com.pos.empressa.empressa_pos.bean.TlvBean;
import com.pos.empressa.empressa_pos.util.TlvUtils;
import com.sunyard.middleware.tlv.Tlv;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import socsi.middleware.emvl2lib.EmvAidCandidate;
import socsi.middleware.emvl2lib.EmvApi;
import socsi.middleware.emvl2lib.EmvCallback;
import socsi.middleware.emvl2lib.EmvCallbackGetPinResult;
import socsi.middleware.emvl2lib.EmvErrorCode;
import socsi.middleware.emvl2lib.EmvStartProcessParam;
import socsi.middleware.emvl2lib.EmvTermConfig;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** EmpressaPosPlugin */
public class EmpressaPosPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;
  ReadCardUtilities readCardUtilities ;
  private Context mContext;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "empressa_pos");
    channel.setMethodCallHandler(this);

  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    readCardUtilities = new ReadCardUtilities(mContext);
    switch (call.method) {
      case "searchCard":
        readCardUtilities.searchCard(result,call.argument("transactionAmount"));
        break;
      case "stopSearch":
        readCardUtilities.stopSearch();
        break;
        case "initEmv":
          MainApplication  mainApplication = new MainApplication();
          mainApplication.initializeApp(mContext);
        break;
        case "startPrinter":
          PrinterUtilities printerUtilities = new PrinterUtilities(mContext);
          Log.d("PrintActivity.class", call.arguments.toString());
          printerUtilities.startPrint(call);
        break;
        case "checkSunyardCard":
          readCardUtilities.checkCard(result);
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
    readCardUtilities.stopSearch();
    // TODO: your plugin is no longer associated with an Activity. Clean up references.

  }

}
