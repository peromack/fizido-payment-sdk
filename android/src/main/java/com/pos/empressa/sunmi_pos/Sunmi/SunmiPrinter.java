package com.pos.empressa.sunmi_pos.Sunmi;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import io.flutter.plugin.common.MethodCall;
import com.pos.empressa.sunmi_pos.Sunmi.utils.ToastUtil;
import com.sunmi.peripheral.printer.InnerResultCallbcak;
import com.sunmi.peripheral.printer.SunmiPrinterService;
import java.util.Objects;


public class SunmiPrinter {

    private Context mContext;
    private SunmiPrinterService sunmiPrinterService;

    private final ToastUtil toastUtil;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

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
//            printHeaderCenter(call.argument("vendorName"));
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
            printTextLeft(call, "senderAccountName", "Sender");
            printTextLeft(call, "senderBankName", "Sender Bank");
            printTextLeft(call, "senderAccountNumber", "Sender Account");
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

    private boolean is = true;
    private final InnerResultCallbcak innerResultCallback = new InnerResultCallbcak() {
        @Override
        public void onRunResult(boolean isSuccess) {
            if(is){
                try {
                    if (isSuccess) {
                        mHandler.post(() -> toastUtil.toast.showToast("print success."));
                    } else {
                        mHandler.post(() -> toastUtil.toast.showToast("print error."));
                    }
                    is = false;
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
        byte[] imageData = call.argument(key);
        int defaultDimension = 220;
        if (imageData != null) {
            try {
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                Bitmap scaledBitmap;
                String width = call.argument("width");
                String height = call.argument("height");
                if(width != null && height != null) {
                    int widthInt = Integer.parseInt(width);
                    int heightInt = Integer.parseInt(height);
                    scaledBitmap = Bitmap.createScaledBitmap(bitmap, widthInt, heightInt, false);
                } else if (width == null && height != null) {
                    int heightInt = Integer.parseInt(height);
                    scaledBitmap = Bitmap.createScaledBitmap(bitmap, defaultDimension, heightInt, false);
                } else if (width != null) {
                    int widthInt = Integer.parseInt(width);
                    scaledBitmap = Bitmap.createScaledBitmap(bitmap, widthInt, defaultDimension, false);
                } else {
                    scaledBitmap = Bitmap.createScaledBitmap(bitmap, defaultDimension, defaultDimension, false);
                }

                sunmiPrinterService.setAlignment(1, innerResultCallback);
                sunmiPrinterService.printBitmapCustom(scaledBitmap, 2, innerResultCallback);
                sunmiPrinterService.lineWrap(1, innerResultCallback);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private void printFooter(@NonNull MethodCall call) {
        try {
            if (call.argument("footer") != null) {
                sunmiPrinterService.setAlignment(1, innerResultCallback);
                sunmiPrinterService.printText(call.argument("footer") + "\n\n", innerResultCallback);
            } else {
                sunmiPrinterService.setAlignment(1, innerResultCallback);
                sunmiPrinterService.printText("Built on Fizido" + "\n\n\n\n", innerResultCallback);
                sunmiPrinterService.printText("Powered by Support MFB" + "\n\n\", innerResultCallback);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
