package com.pos.empressa.empressa_pos.Nexgo;

import android.content.Context;
import android.graphics.Typeface;

import androidx.annotation.NonNull;

import com.nexgo.oaf.apiv3.APIProxy;
import com.nexgo.oaf.apiv3.DeviceEngine;
import com.nexgo.oaf.apiv3.device.printer.AlignEnum;
import com.nexgo.oaf.apiv3.device.printer.DotMatrixFontEnum;
import com.nexgo.oaf.apiv3.device.printer.FontEntity;
import com.nexgo.oaf.apiv3.device.printer.OnPrintListener;
import com.nexgo.oaf.apiv3.device.printer.Printer;

import io.flutter.plugin.common.MethodCall;

public class NexgoPrinter {

    private DeviceEngine deviceEngine;
    private Printer printer;
    private final int FONT_SIZE_SMALL = 20;
    private final int FONT_SIZE_NORMAL = 24;
    private final int FONT_SIZE_BIG = 24;
    private FontEntity fontSmall = new FontEntity(DotMatrixFontEnum.CH_SONG_20X20, DotMatrixFontEnum.ASC_SONG_8X16);
    private FontEntity fontNormal = new FontEntity(DotMatrixFontEnum.CH_SONG_24X24, DotMatrixFontEnum.ASC_SONG_12X24);
    private FontEntity fontBold = new FontEntity(DotMatrixFontEnum.CH_SONG_24X24, DotMatrixFontEnum.ASC_SONG_BOLD_16X24);
    private FontEntity fontBig = new FontEntity(DotMatrixFontEnum.CH_SONG_24X24, DotMatrixFontEnum.ASC_SONG_12X24, false, true);

    private Context mContext;

    public NexgoPrinter (Context mContext) {
        initializePrinter(mContext);
        this.mContext = mContext;
    }

    private void initializePrinter(Context mContext) {
        deviceEngine = APIProxy.getDeviceEngine(mContext);
        printer = deviceEngine.getPrinter();
        printer.setTypeface(Typeface.DEFAULT);
    }


    public void nexgoPrint(@NonNull MethodCall call) {
        printer.initPrinter();
        printer.setLetterSpacing(5);
        printer.appendPrnStr("Transaction Receipt", fontBig, AlignEnum.CENTER);
        printer.appendPrnStr("---------------------------", fontNormal, AlignEnum.LEFT);
        printer.appendPrnStr("AMOUNT: NGN " +call.argument("originalMinorAmount").toString(), fontNormal, AlignEnum.LEFT);
        printer.appendPrnStr("---------------------------", fontNormal, AlignEnum.LEFT);
        printer.appendPrnStr("TerminalID: "+call.argument("terminalId"), fontNormal, AlignEnum.LEFT);
        printer.appendPrnStr("MerchantID: "+call.argument("merchantId"), fontNormal, AlignEnum.LEFT);
        printer.appendPrnStr("Stan: "+call.argument("originalTransStan"), fontNormal, AlignEnum.LEFT);
        printer.appendPrnStr("Date: "+ call.argument("transmissionDate"), fontNormal, AlignEnum.LEFT);
        printer.appendPrnStr("Card PAN:    "+call.argument("cardPan"), fontBold, AlignEnum.LEFT);
        printer.appendPrnStr("Card Holder: "+call.argument("cardHolder"), fontNormal, AlignEnum.LEFT);
        printer.appendPrnStr("Card Expiry: "+call.argument("expiryDate"), fontNormal, AlignEnum.LEFT);
        printer.appendPrnStr("Ref: "+call.argument("transactionRef"), fontNormal, AlignEnum.LEFT);
        printer.appendPrnStr(call.argument("transactionComment"), fontNormal, AlignEnum.LEFT);
        printer.appendPrnStr("\n", fontNormal, AlignEnum.LEFT);
        printer.appendPrnStr("\n", fontNormal, AlignEnum.LEFT);
        printer.appendPrnStr("\n", fontNormal, AlignEnum.LEFT);
        printer.appendPrnStr("---------------------------", fontNormal, AlignEnum.LEFT);
        printer.appendPrnStr("I ACKNOWLEDGE SATISFACTORY RECEIPT OF RELATIVE GOODS/SERVICES", fontSmall, AlignEnum.LEFT);
        printer.appendPrnStr("---------------------------", fontNormal, AlignEnum.LEFT);
        printer.startPrint(true, new OnPrintListener() {
            @Override
            public void onPrintResult(final int retCode) {
            }
        });
    }

}
