package com.pos.empressa.nexgo_pos.Nexgo;

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
    private FontEntity fontBold = new FontEntity(DotMatrixFontEnum.CH_SONG_24X24,
            DotMatrixFontEnum.ASC_SONG_BOLD_16X24);
    private FontEntity fontBig = new FontEntity(DotMatrixFontEnum.CH_SONG_24X24, DotMatrixFontEnum.ASC_SONG_12X24,
            false, true);

    private Context mContext;

    public NexgoPrinter(Context mContext) {
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
        printer.appendPrnStr(call.argument("vendorName"), fontNormal, AlignEnum.CENTER);
        printer.appendPrnStr("Transaction Receipt", fontBig, AlignEnum.CENTER);
        printer.appendPrnStr("--------------------------------", fontNormal, AlignEnum.CENTER);
        printer.appendPrnStr("NGN" + call.argument("originalMinorAmount").toString(), fontNormal, AlignEnum.CENTER);
        printer.appendPrnStr("--------------------------------", fontNormal, AlignEnum.CENTER);
        printText(call, "merchantName", "Merchant Name");
        printText(call, "merchantLocation", "Merchant Location");
        printText(call, "sender", "Sender");
        printText(call, "receiver", "Receiver");
        printText(call, "transactionType", "Transaction Type");
        printText(call, "payee", "Payee");
        printText(call, "phoneNumber", "Phone Number");
        printText(call, "service", "Service");
        printText(call, "beneficiaryName", "Beneficiary Name");
        printText(call, "bankName", "Bank Name");
        printText(call, "senderAccountName", "Sender");
        printText(call, "senderBankName", "Sender Bank");
        printText(call, "senderAccountNumber", "Sender Account");
        printText(call, "merchant", "Merchant");
        printText(call, "accountNumber", "Account Number");
        printText(call, "description", "Description");
        printText(call, "bill", "Bill");
        printText(call, "paymentItem", "Payment Item");
        printText(call, "billItem", "Bill Item");
        printText(call, "qty", "Qty");
        printText(call, "packageName", "Package Name");
        printText(call, "customerName", "Customer Name");
        printText(call, "customerId", "Customer Id");
        printText(call, "customerReference", "Customer Reference");
        printText(call, "transactionFee", "Transaction Fee");
        printText(call, "transactionRef", "Reference Number");
        printText(call, "originalTransStan", "Stan");
        printText(call, "cardPan", "Card PAN");
        printText(call, "tokenValue", "Token");
        printText(call, "expiryDate", "Card Expiry");
        printText(call, "terminalId", "TerminalID");
        printText(call, "merchantId", "MerchantID");
        printText(call, "agent", "Agent");
        printText(call, "time", "Time");
        printText(call, "transmissionDate", "Date");
        printer.appendPrnStr("--------------------------------", fontNormal, AlignEnum.CENTER);
        printer.appendPrnStr(call.argument("transactionComment"), fontNormal, AlignEnum.CENTER);
        printFooter(call, "footer");
        printer.appendPrnStr("\n", fontNormal, AlignEnum.LEFT);
        printer.appendPrnStr("--------------------------------", fontNormal, AlignEnum.CENTER);
        printer.startPrint(true, new OnPrintListener() {
            @Override
            public void onPrintResult(final int retCode) {
            }
        });
    }

    private void printText(@NonNull MethodCall call, String key, String title) {
        if (call.argument(key) != null) {
            printer.appendPrnStr(title + ": " + call.argument(key) + "\n", fontNormal, AlignEnum.LEFT);
        }
    }

    private void printFooter(@NonNull MethodCall call, String key) {
        if (call.argument(key) != null) {
            printer.appendPrnStr(call.argument(key) + "\n\n", fontNormal, AlignEnum.CENTER);
        } else {
            printer.appendPrnStr("Built on Fizido, Powered by Support MFB" + "\n\n", fontNormal, AlignEnum.LEFT);
        }
    }

}
