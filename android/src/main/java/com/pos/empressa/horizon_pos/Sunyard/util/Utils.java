package com.pos.empressa.horizon_pos.Sunyard.util;

import android.content.Context;
import android.widget.Toast;

public class Utils {
    public static void popToast(Context c, String s) {
        Toast.makeText(c, s, Toast.LENGTH_SHORT).show();
    }
}
