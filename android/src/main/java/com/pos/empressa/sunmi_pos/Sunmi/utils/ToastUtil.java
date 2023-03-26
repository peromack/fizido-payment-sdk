package com.pos.empressa.sunmi_pos.Sunmi.utils;

import android.content.Context;
import android.widget.Toast;

public class ToastUtil {

    public ToastUtil toast;

    private Context mContext;

    public ToastUtil(Context mContext) {
        toast = this;
        this.mContext = mContext;
    }

    public void showToast(String msg) {
        Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
    }
}
