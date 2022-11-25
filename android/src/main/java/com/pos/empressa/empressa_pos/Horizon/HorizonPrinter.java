package com.pos.empressa.empressa_pos.Horizon;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.widget.Toast;
import androidx.annotation.NonNull;

import com.horizonpay.smartpossdk.aidl.printer.AidlPrinterListener;
import com.horizonpay.smartpossdk.aidl.printer.IAidlPrinter;
import com.horizonpay.smartpossdk.data.PrinterConst;
import com.pos.empressa.empressa_pos.Horizon.utils.CombBitmap;
import com.pos.empressa.empressa_pos.Horizon.utils.GenerateBitmap;

import io.flutter.plugin.common.MethodCall;

public class HorizonPrinter {

    private static final String TAG = "HorizonPrinter";
    Handler handler = new Handler(Looper.getMainLooper());
    IAidlPrinter printer;

    Context mContext;

    public HorizonPrinter (Context context) {
        try {
            printer = DeviceHelper.getPrinter();
            mContext = context;
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }

    }

    private void setPrintLevel(int level){
        try {
            printer.setPrintGray(level);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    public void print(@NonNull MethodCall call) {
        try {
            setPrintLevel(PrinterConst.Gray.LEVEL_3);

            printer.printBmp(true, false, generateTestBitmap(call), 0, new AidlPrinterListener.Stub() {
                @Override
                public void onError(int i) throws RemoteException {
                    switch (i) {
                        case PrinterConst.RetCode.ERROR_PRINT_NOPAPER:
                            ToastHelper("No paper present");
                            break;
                        case PrinterConst.RetCode.ERROR_DEV:
                            ToastHelper("Error printing");
                            break;
                        case PrinterConst.RetCode.ERROR_DEV_IS_BUSY:
                            ToastHelper("Printer is busy");
                            break;
                        default:
                        case PrinterConst.RetCode.ERROR_OTHER:
                            ToastHelper("Error printing");
                            break;
                    }
                }

                @Override
                public void onPrintSuccess() throws RemoteException {
                    ToastHelper("print success");
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void ToastHelper(String text) {
        handler.post(
                () -> Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show()
        );
    }


    private Bitmap generateTestBitmap(@NonNull MethodCall call) {
        CombBitmap combBitmap = new CombBitmap();
        //Title
        combBitmap.addBitmap(GenerateBitmap.str2Bitmap(call.argument("vendorName"), 26, GenerateBitmap.AlignEnum.CENTER, true, false));
        combBitmap.addBitmap(GenerateBitmap.str2Bitmap("Transaction Receipt", 26, GenerateBitmap.AlignEnum.CENTER, true, false));

        combBitmap.addBitmap(GenerateBitmap.generateLine(1)); // print one line
        combBitmap.addBitmap(GenerateBitmap.str2Bitmap("AMOUNT: NGN" +call.argument("originalMinorAmount").toString(), 26, GenerateBitmap.AlignEnum.LEFT, true, false));
        combBitmap.addBitmap(GenerateBitmap.generateLine(1)); // print one line

        printText(call, "merchantName", "Merchant Name", combBitmap);
        printText(call, "merchantLocation", "Merchant Location", combBitmap);
        printText(call, "sender", "Sender", combBitmap);
        printText(call, "receiver", "Receiver", combBitmap);
        printText(call, "transactionType", "Transaction Type", combBitmap);
        printText(call, "payee", "Payee", combBitmap);
        printText(call, "phoneNumber", "Phone Number", combBitmap);
        printText(call, "service", "Service", combBitmap);
        printText(call, "beneficiaryName", "Beneficiary Name", combBitmap);
        printText(call, "bankName", "Bank Name", combBitmap);
        printText(call, "merchant", "Merchant", combBitmap);
        printText(call, "accountNumber", "Account Number", combBitmap);
        printText(call, "description", "Description", combBitmap);
        printText(call, "bill", "Bill", combBitmap);
        printText(call, "paymentItem", "Payment Item", combBitmap);
        printText(call, "billItem", "Bill Item", combBitmap);
        printText(call, "qty", "Qty", combBitmap);
        printText(call, "packageName", "Package Name", combBitmap);
        printText(call, "customerName", "Customer Name", combBitmap);
        printText(call, "customerId", "Customer Id", combBitmap);
        printText(call, "customerReference", "Customer Reference", combBitmap);
        printText(call, "transactionFee", "Transaction Fee", combBitmap);
        printText(call, "transactionRef", "Reference Number", combBitmap);
        printText(call, "originalTransStan", "Stan", combBitmap);
        printText(call, "cardPan", "Card PAN", combBitmap);
        printText(call, "tokenValue", "Token", combBitmap);
        printText(call, "expiryDate", "Card Expiry", combBitmap);
        printText(call, "terminalId", "TerminalID", combBitmap);
        printText(call, "merchantId", "MerchantID", combBitmap);
        printText(call, "agent", "Agent", combBitmap);
        printText(call, "time", "Time", combBitmap);
        printText(call, "transmissionDate", "Date", combBitmap);

        combBitmap.addBitmap(GenerateBitmap.generateLine(1)); // print one line

        //Content
        combBitmap.addBitmap(GenerateBitmap.str2Bitmap(call.argument("transactionComment"), 26, GenerateBitmap.AlignEnum.LEFT, true, false));
        printFooter(call, "footer", combBitmap);
        combBitmap.addBitmap(GenerateBitmap.str2Bitmap("\n", 26, GenerateBitmap.AlignEnum.LEFT, true, false));
        combBitmap.addBitmap(GenerateBitmap.str2Bitmap("\n", 26, GenerateBitmap.AlignEnum.LEFT, true, false));
        combBitmap.addBitmap(GenerateBitmap.str2Bitmap("\n", 26, GenerateBitmap.AlignEnum.LEFT, true, false));

        combBitmap.addBitmap(GenerateBitmap.str2Bitmap("--------------------------------------", 20, GenerateBitmap.AlignEnum.CENTER, true, false)); // 打印一行直线

        Bitmap bp = combBitmap.getCombBitmap();

        return bp;
    }

    private void printText(@NonNull MethodCall call, String key, String title, CombBitmap combBitmap) {
        if (call.argument(key) != null) {
            combBitmap.addBitmap(GenerateBitmap.str2Bitmap(title + ": " + call.argument(key), 26, GenerateBitmap.AlignEnum.LEFT, false, false));
        }
    }

    private void printFooter(@NonNull MethodCall call, String key, CombBitmap combBitmap) {
        if (call.argument(key) != null) {
            combBitmap.addBitmap(GenerateBitmap.str2Bitmap(call.argument(key) + "\n\n\n\n", 26, GenerateBitmap.AlignEnum.LEFT, false, false));
        } else {
            combBitmap.addBitmap(GenerateBitmap.str2Bitmap("Built on Fizido, Powered by Support MFB" + "\n\n\n\n", 26, GenerateBitmap.AlignEnum.LEFT, false, false));
        }
    }
}
