package com.pos.empressa.empressa_pos.Sunyard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.socsi.exception.SDKException;
import com.socsi.smartposapi.printer.Align;
import com.socsi.smartposapi.printer.FontLattice;
import com.socsi.smartposapi.printer.FontType;
import com.socsi.smartposapi.printer.Printer2;
import com.socsi.smartposapi.printer.TextEntity;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.flutter.plugin.common.MethodCall;


public class SunyardPrinter {

    private Printer2 print;
    private FontType mCh = FontType.SIMSUM, mEn = FontType.AVENIR_NEXT_CONDENSED_BLOD;

    public SunyardPrinter(Context context) {
        initPrinter(context);
    }


    private void initPrinter(Context context){
         print = Printer2.getInstance(context);
         boolean havePrinter = false;
         try {
             havePrinter = print.havePrinter();
         } catch (SDKException e) {
             e.printStackTrace();
         }
         if (havePrinter) {
            // Utils.popToast(context, "pos Supports printing");

         }
     }

    public void startPrint(@NonNull MethodCall call) {
        //String stan, int originalMinorAmount, String terminalId, String merchantId, String transmissionDate, String transactionComment
        Bitmap bitmap = BitmapFactory.decodeResource();
        Bitmap bitmapForWatermark  = print.createBitmapForWatermark(bitmap);
        print.appendImage(bitmap);
        print.appendImage(bitmapForWatermark, Align.CENTER);
        DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");

        Date today = new Date();
        Date todayWithZeroTime = new Date();

        try {
             todayWithZeroTime = formatter.parse(formatter.format(today));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        print.appendTextEntity2(new TextEntity("Transaction Receipt", mCh, mEn, FontLattice.THIRTY, false, Align.CENTER,true));
        print.appendTextEntity2(print.getSeparatorLinetEntity());
        print.appendTextEntity2(new TextEntity("TerminalID: "+call.argument("terminalId"), mCh, mEn, FontLattice.TWENTY_FOUR, false, Align.LEFT,true));
        print.appendTextEntity2(new TextEntity("MerchantID: "+call.argument("merchantId"), mCh, mEn, FontLattice.TWENTY_FOUR, false, Align.LEFT,true));
        print.appendTextEntity2(new TextEntity("Stan:   "+call.argument("originalTransStan"), mCh, mEn, FontLattice.TWENTY_FOUR, false, Align.LEFT,true));
        print.appendTextEntity2(new TextEntity("Amount: NGN "+call.argument("originalMinorAmount").toString(), mCh, mEn, FontLattice.TWENTY_FOUR, false, Align.LEFT,true));
        print.appendTextEntity2(new TextEntity("Date:   "+todayWithZeroTime.toString().substring(0, 10), mCh, mEn, FontLattice.TWENTY_FOUR, false, Align.LEFT,true));
        print.appendTextEntity2(new TextEntity("Comment:    "+call.argument("transactionComment"), mCh, mEn, FontLattice.TWENTY_FOUR, false, Align.LEFT,true));
        print.appendTextEntity2(print.getSeparatorLinetEntity());
        print.limitTimePrint(10, print.getPrintBuffer());
        print.startPrint();
    }

}
