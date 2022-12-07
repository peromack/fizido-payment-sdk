package com.pos.empressa.horizon_pos.Horizon.dialog;


import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AlertDialog;
import com.horizonpay.smartpossdk.aidl.emv.CandidateAID;

import java.util.List;
import java.util.concurrent.Callable;

/***************************************************************************************************
 *                          Copyright (C),  Shenzhen Horizon Technology Limited                    *
 *                                   http://www.horizonpay.cn                                      *
 ***************************************************************************************************
 * usage           :
 * Version         : 1
 * Author          : Ashur Liu
 * Date            : 2017/12/18
 * Modify          : create file
 **************************************************************************************************/
public class AppSelectDialog implements Callable<Integer> {

    private int mSelectedIndex = -1;
    private int mLoopCount = 0;
    private AlertDialog mAlertDialog;

    public AppSelectDialog(final Context context, final List<CandidateAID> candidateList) {
//        final String[] items = new String[candidateList.size()];
//        for (int i = 0; i < candidateList.size(); i++) {
//            CandidateAID candidate = candidateList.get(i);
//            items[i] = new String(candidate.getAppLabel());
//        }
//
//        new Handler(Looper.getMainLooper()).post(new Runnable() {
//            @Override
//            public void run() {
//                AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppTheme_Dialog);
//                builder.setCancelable(false);
//                builder.setTitle(R.string.dialog_app_select_title);
//                builder.setItems(items, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        mSelectedIndex = which;
//                    }
//                });
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
//                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
//                        @Override
//                        public void onDismiss(DialogInterface dialog) {
//                            // 如果用户不需要而关闭对话框，默认选择第一个
//                            if (mSelectedIndex == -1) {
//                                mSelectedIndex = 0;
//                            }
//                        }
//                    });
//                }
//                mAlertDialog = builder.create();
//                /*if (mAlertDialog.getWindow() != null) {
//                    mAlertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
//                }*/
//                mAlertDialog.show();
//            }
//        });
    }

    @Override
    public Integer call() throws Exception {
        while (mSelectedIndex == -1) {
            Thread.sleep(500);
            mLoopCount++;
            if (mLoopCount > 10) {
                mSelectedIndex = 1;
            }
        }

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (mAlertDialog != null && mAlertDialog.isShowing()) {
                    mAlertDialog.dismiss();
                    mAlertDialog = null;
                }
            }
        });

        return mSelectedIndex;
    }

}
