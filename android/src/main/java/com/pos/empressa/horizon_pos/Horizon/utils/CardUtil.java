package com.pos.empressa.horizon_pos.Horizon.utils;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class CardUtil {
    private static final String TAG = "CardUtil";

    public static String getCardTypFromAid(String aid) {
        if (aid == null || aid.length() < 10) {
            return "";
        }
        Log.d(TAG, "getCardTypFromAid: " + aid.length());
        if (cardType.containsKey(aid.substring(0, 10))) {
            return cardType.get(aid.substring(0, 10));
        }
        return "";
    }

    private static Map<String, String> cardType = new HashMap<String, String>();

    static {
        cardType.put("A000000004", "MASTER");
        cardType.put("A000000003", "VISA");
        cardType.put("A000000025", "AMEX");
        cardType.put("A000000065", "JCB");
        cardType.put("A000000152", "DISCOVER");
        cardType.put("A000000324", "DISCOVER");
        cardType.put("A000000333", "PBOC");
        cardType.put("A000000524", "RUPAY");
    }


    public static String getCurrencyName(String code) {
        try {
            if (cardCurrency.containsKey(code.substring(0, 3))) {
                return cardCurrency.get(code.substring(0, 3));
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return "UnKnown";
    }

    private static Map<String, String> cardCurrency = new HashMap<String, String>();

    static {
        cardCurrency.put("156", "RMB");
        cardCurrency.put("344", "HKD");
        cardCurrency.put("446", "MOP");
        cardCurrency.put("458", "MYR");
        cardCurrency.put("702", "SGD");
        cardCurrency.put("978", "EUR");
        cardCurrency.put("036", "AUD");
        cardCurrency.put("764", "THB");
        cardCurrency.put("784", "AED");
        cardCurrency.put("392", "JPY");
        cardCurrency.put("360", "IDR");
        cardCurrency.put("840", "USD");
        cardCurrency.put("566", "NGN");
        cardCurrency.put("356", "INR");
        cardCurrency.put("364", "IRR");
        cardCurrency.put("400", "JOD");
        cardCurrency.put("116", "KHR");
        cardCurrency.put("480", "MUR");
        cardCurrency.put("938", "SDG");
    }

}
