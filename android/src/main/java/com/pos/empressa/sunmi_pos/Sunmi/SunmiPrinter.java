package com.pos.empressa.sunmi_pos.Sunmi;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.pos.empressa.sunmi_pos.Sunmi.utils.ToastUtil;
import com.sunmi.peripheral.printer.InnerResultCallbcak;
import com.sunmi.peripheral.printer.SunmiPrinterService;

import java.util.Objects;

import io.flutter.plugin.common.MethodCall;

public class SunmiPrinter {

    private Context mContext;
    private SunmiPrinterService sunmiPrinterService;

    private ToastUtil toastUtil;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    public SunmiPrinter(Context mContext) {
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

            printHeaderCenter("\n");
            printLogo(call, "logo");
            printHeaderCenter(call.argument("vendorName"));
            printHeaderCenter("****Customer Copy****");
            printHeaderCenter("Transaction Receipt");
            printHeaderCenter("--------------------------------");
            printTextCenter(call, "originalMinorAmount", "AMOUNT");
            printHeaderCenter("--------------------------------");
            printTextLeft(call, "merchantName", "Merchant Name");
            printTextLeft(call, "merchantLocation", "Merchant Location");
            printTextLeft(call, "sender", "Sender");
            printTextLeft(call, "receiver", "Receiver");
            printTextLeft(call, "transactionType", "Transaction Type");
            printTextLeft(call, "payee", "Payee");
            printTextLeft(call, "phoneNumber", "Phone Number");
            printTextLeft(call, "service", "Service");
            printTextLeft(call, "beneficiaryName", "Beneficiary Name");
            printTextLeft(call, "bankName", "Bank Name");
            printTextLeft(call, "merchant", "Merchant");
            printTextLeft(call, "accountNumber", "Account Number");
            printTextLeft(call, "description", "Description");
            printTextLeft(call, "bill", "Bill");
            printTextLeft(call, "paymentItem", "Payment Item");
            printTextLeft(call, "billItem", "Bill Item");
            printTextLeft(call, "qty", "Qty");
            printTextLeft(call, "packageName", "Package Name");
            printTextLeft(call, "customerName", "Customer Name");
            printTextLeft(call, "customerId", "Customer Id");
            printTextLeft(call, "customerReference", "Customer Reference");
            printTextLeft(call, "transactionFee", "Transaction Fee");
            printTextLeft(call, "transactionRef", "Reference Number");
            printTextLeft(call, "originalTransStan", "Stan");
            printTextLeft(call, "cardPan", "Card PAN");
            printTextLeft(call, "tokenValue", "Token");
            printTextLeft(call, "expiryDate", "Card Expiry");
            printTextLeft(call, "terminalId", "TerminalID");
            printTextLeft(call, "merchantId", "MerchantID");
            printTextLeft(call, "agent", "Agent");
            printTextLeft(call, "time", "Time");
            printTextLeft(call, "transmissionDate", "Date");
            printHeaderCenter("--------------------------------");
            printTextCenter(call, "transactionComment", "Transaction");
            printFooter(call);
            printHeaderCenter("\n");
            printHeaderCenter("\n");
            printHeaderCenter("\n");
            sunmiPrinterService.printText("--------------------------------" + "\n", innerResultCallback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final InnerResultCallbcak innerResultCallback = new InnerResultCallbcak() {
        @Override
        public void onRunResult(boolean isSuccess) {
            if (isSuccess) {
                mHandler.post(() -> toastUtil.toast.showToast("print success."));
            } else {
                mHandler.post(() -> toastUtil.toast.showToast("print error."));
            }
        }

        @Override
        public void onReturnString(String result) {
        }

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
                sunmiPrinterService.printText(title + ": " + call.argument(key) + "\n", innerResultCallback);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void printTextLeft(@NonNull MethodCall call, String key, String title) {
        if (call.argument(key) != null) {
            try {
                sunmiPrinterService.setAlignment(0, innerResultCallback);
                sunmiPrinterService.printText(title + ": " + call.argument(key) + "\n", innerResultCallback);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void printTextCenter(@NonNull MethodCall call, String key, String title) {
        if (call.argument(key) != null) {
            try {
                sunmiPrinterService.setAlignment(1, innerResultCallback);
                if(Objects.equals(title, "Transaction")) {
                    sunmiPrinterService.printText(title + " " + call.argument(key) + "\n", innerResultCallback);
                } else {
                    sunmiPrinterService.printText(title + ": " + call.argument(key) + "\n", innerResultCallback);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void printHeaderCenter(String header) {
        try {
            sunmiPrinterService.setAlignment(1, innerResultCallback);
            sunmiPrinterService.printText(header + "\n", innerResultCallback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void printLogo(MethodCall call, String key) {
        byte[] byteArray = call.argument(key);
        if (byteArray != null) {
            try {
                Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                int originalWidth = bitmap.getWidth();
                int originalHeight = bitmap.getHeight();

                float aspectRatio = (float) originalWidth / (float) originalHeight;

                int newWidth = 384;
                int newHeight = Math.round(newWidth / aspectRatio);

                Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false);
                sunmiPrinterService.printBitmap(resizedBitmap, innerResultCallback);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void printFooter(@NonNull MethodCall call) {
        try {
            if (call.argument("footer") != null) {
                sunmiPrinterService.setAlignment(1, innerResultCallback);
                sunmiPrinterService.printText(call.argument("footer") + "\n\n\n\n", innerResultCallback);
            } else {
                sunmiPrinterService.setAlignment(1, innerResultCallback);
                sunmiPrinterService.printText("Built on Fizido" + "\n\n\n\n", innerResultCallback);
                sunmiPrinterService.printText("Powered by Support MFB" + "\n\n\n\n", innerResultCallback);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
