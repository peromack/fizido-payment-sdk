package com.pos.empressa.sunmi_pos.Sunmi;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.pos.empressa.sunmi_pos.Sunmi.utils.ToastUtil;
import com.sunmi.peripheral.printer.InnerResultCallbcak;
import com.sunmi.peripheral.printer.SunmiPrinterService;

import io.flutter.plugin.common.MethodCall;

public class SunmiPrinter {

    private Context mContext;
    private SunmiPrinterService sunmiPrinterService;

    private ToastUtil toastUtil;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    public SunmiPrinter (Context mContext) {
        initializePrinter();
        this.mContext = mContext;
        toastUtil = new ToastUtil(mContext);
    }

    private void initializePrinter() {
        sunmiPrinterService = SunmiApplication.app.sunmiPrinterService;
    }

    private boolean checkPrint() {
        if (SunmiApplication.app.sunmiPrinterService == null) {
            mHandler.post(() -> toastUtil.toast.showToast("print not supported."));
            return false;
        }
        return true;
    }

    public void sunmiPrint(@NonNull MethodCall call) {
        try {
            if (!checkPrint()) {
                return;
            }
            sunmiPrinterService.printText("\n", innerResultCallbcak);
            sunmiPrinterService.printText(call.argument("vendorName") + "\n", innerResultCallbcak);
            sunmiPrinterService.printText("Transaction Receipt" + "\n", innerResultCallbcak);
            sunmiPrinterService.printText("---------------------------" + "\n", innerResultCallbcak);
            sunmiPrinterService.printText("AMOUNT: NGN" +call.argument("originalMinorAmount").toString() + "\n", innerResultCallbcak);
            sunmiPrinterService.printText("---------------------------" + "\n", innerResultCallbcak);
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
            sunmiPrinterService.printText("---------------------------" + "\n", innerResultCallbcak);
            sunmiPrinterService.printText("Comment: " + call.argument("transactionComment") + "\n", innerResultCallbcak);
            printFooter(call);
            sunmiPrinterService.printText("\n", innerResultCallbcak);
            sunmiPrinterService.printText("\n", innerResultCallbcak);
            sunmiPrinterService.printText("\n", innerResultCallbcak);
            sunmiPrinterService.printText("---------------------------" + "\n", innerResultCallbcak);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final InnerResultCallbcak innerResultCallbcak = new InnerResultCallbcak() {
        @Override
        public void onRunResult(boolean isSuccess) {
            if(isSuccess) {
                mHandler.post(() -> toastUtil.toast.showToast("print success."));
            } else {
                mHandler.post(() -> toastUtil.toast.showToast("print error."));
            }
        }

        @Override
        public void onReturnString(String result) {}

        @Override
        public void onRaiseException(int code, String msg) {
        }

        @Override
        public void onPrintResult(int code, String msg) {
        }
    };

    private void printText(@NonNull MethodCall call, String key, String title) {
        if (call.argument(key) != null) {
            try {
                sunmiPrinterService.printText(title + ": " + call.argument(key) + "\n", innerResultCallbcak);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void printFooter(@NonNull MethodCall call) {
        try {
            if (call.argument("footer") != null) {
                sunmiPrinterService.printText(call.argument("footer") + "\n\n\n\n", innerResultCallbcak);
            } else {
                sunmiPrinterService.printText("Built on Fizido, Powered by Support MFB" + "\n\n\n\n", innerResultCallbcak);
            }
        } catch (Exception e ){
            e.printStackTrace();
        }

    }
}
