package com.pos.empressa.nexgo_pos.MPos.utils;

import android.app.Activity;
import android.graphics.Color;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class SweetDialogUtils {
    private static SweetAlertDialog pDialog;

    public static void showNormal(Activity context, String contextText) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new SweetAlertDialog(context, SweetAlertDialog.NORMAL_TYPE)
                        .setContentText(contextText)
                        .show();
            }
        });

    }

    public static void showSuccess(Activity context, String contextText) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new SweetAlertDialog(context, SweetAlertDialog.SUCCESS_TYPE)
                        .setContentText(contextText)
                        .show();
            }
        });

    }

    public static void showError(Activity context, String contextText) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new SweetAlertDialog(context, SweetAlertDialog.ERROR_TYPE)
                        .setContentText(contextText)
                        .show();
            }
        });

    }

    public static void showProgress(Activity context, String text, boolean cancelable) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pDialog = new SweetAlertDialog(context, SweetAlertDialog.PROGRESS_TYPE);
                pDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
                pDialog.setTitleText(text);
                pDialog.setCancelable(cancelable);
                pDialog.show();
            }
        });
    }

    public static void changeAlertType(Activity activity, String text, int type) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (pDialog != null) {
                    pDialog.changeAlertType(type);
                    pDialog.setTitleText(text);
                }
                pDialog = null;
            }
        });
    }

    public static void setProgressText(Activity activity, String text) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pDialog.setTitleText(text);
            }
        });
    }
}
