package com.pos.empressa.empressa_pos;

import android.content.Context;


import com.socsi.exception.SDKException;
import com.socsi.smartposapi.printer.Align;
import com.socsi.smartposapi.printer.FontLattice;
import com.socsi.smartposapi.printer.FontType;
import com.socsi.smartposapi.printer.Printer2;
import com.socsi.smartposapi.printer.TextEntity;



public class PrinterUtilities {

    private Printer2 print;
    private FontType mCh = FontType.SIMSUM, mEn = FontType.AVENIR_NEXT_CONDENSED_BLOD;

    public PrinterUtilities(Context context) {
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

    public void startPrint() {
        print.appendTextEntity2(new TextEntity("Empressa Pos Receipt", mCh, mEn, FontLattice.FORTY_FOUR, false, Align.LEFT,true));
        print.appendTextEntity2(print.getSeparatorLinetEntity());

        print.addTextInLine(new TextEntity("Empressa PosReciept Test", mCh, mEn, FontLattice.TWENTY_FOUR, false, Align.LEFT,true));

        print.limitTimePrint(10, print.getPrintBuffer());
        print.startPrint();
    }

}
