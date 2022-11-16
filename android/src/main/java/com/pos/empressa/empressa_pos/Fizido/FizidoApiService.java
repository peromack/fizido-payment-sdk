package com.pos.empressa.empressa_pos.Fizido;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.TimeoutError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.socsi.utils.Log;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

public class FizidoApiService {
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void chargeFidizoTransaction(@NonNull MethodChannel.Result result, Context mContext, @NonNull MethodCall call) {
        RequestQueue queue = Volley.newRequestQueue(mContext);

        String url = "http://13.59.82.195:83/api/TransactionsV2/ProcessCardPaymentClearV2";

        JSONObject header = new JSONObject();
        JSONObject transaction = new JSONObject();
        JSONObject ti = new JSONObject();
        JSONObject cd = new JSONObject();
        JSONObject t2 = new JSONObject();
        JSONObject ed = new JSONObject();
        JSONObject pd = new JSONObject();

        try {
            ti.put("bi","-1"); //battery information
            ti.put("cc", 566);
            ti.put("li", "EN");
            ti.put("mid", "07610000000PI91"); //merchantId
            ti.put("mloc", "Lagos, Nigeria"); //merchant location
            ti.put("pcc", "00"); //pos condition code
            ti.put("pdc", "510101511344101");
            ti.put("pem", "051");
            ti.put("pgc", "00234000000000566");
            ti.put("ps", 1);
            ti.put("tid", "2076NA61");
            ti.put("tt", 22);
            String dateTrans = call.argument("originalTransmissionDateAndTime");
            String[] dateTransArr = dateTrans.replace(" ", "T").split("[.]");
            ti.put("transdate", dateTransArr[0]);
            ti.put("ui", "3132333435363738"); //TODO: Ask for the value

            Log.d("ti body", String.valueOf(ti));
        }catch (Exception e){
            e.printStackTrace();
        }

        try {
            ed.put("aa", call.argument("transactionAmount"));
            ed.put("ao", call.argument("anotherAmount"));
            ed.put("aip", Integer.parseInt(Objects.requireNonNull(call.argument("applicationInterchangeProfile"))));
            ed.put("atc", call.argument("applicationTransactionCounter"));
            ed.put("cm",  call.argument("authorizationRequest"));
            ed.put("cid", "80");
            ed.put("cr", Integer.parseInt(Objects.requireNonNull(call.argument("cardholderVerificationResult"))));
            ed.put("iad", call.argument("issuerAppData"));
            ed.put("tcc", Integer.parseInt(Objects.requireNonNull(call.argument("transactionCurrencyCode"))));
            ed.put("tvr", call.argument("terminalVerificationResult"));
            ed.put("termcc", Integer.parseInt(Objects.requireNonNull(call.argument("countryCode"))));
            ed.put("tt", Integer.parseInt(Objects.requireNonNull(call.argument("terminalType"))));
            ed.put("tc", call.argument("terminalCapabilities"));
            ed.put("transdate", call.argument("transactionDate"));
            ed.put("transType", call.argument("transactionType"));
            ed.put("un", call.argument("unpredictableNumber"));
            ed.put("dfn", call.argument("dedicatedFileName"));

            Log.d("ed body", String.valueOf(ed));
        }catch (Exception e){
            e.printStackTrace();
        }

        try {
            String cardMonthYear = call.argument("cardExpiry");

            t2.put("pan", call.argument("cardPAN"));
            t2.put("em", cardMonthYear.substring(2));
            t2.put("ey", cardMonthYear.substring(0,2));
            t2.put("t2", call.argument("cardTrack2"));

            cd.put("csn",call.argument("csn"));
            cd.put("ed", ed);
            cd.put("t2", t2);


            Log.d("cd body", String.valueOf(cd));
        }catch (Exception e){
            e.printStackTrace();
        }

        try {
            transaction.put("ti", ti);
            transaction.put("cd", cd);
            transaction.put("fa", call.argument("accountType"));
            transaction.put("stan", call.argument("originalStan"));
            transaction.put("ma", 1075);
            transaction.put("pd", pd);
            transaction.put("kl","000002" );
            transaction.put("rii", "519911");
            transaction.put("sc", 1075);
            transaction.put("dan", "0003575609");
            transaction.put("ett", 6103);
            transaction.put("rrn","000001359066");

            Log.d("transaction body", String.valueOf(transaction));
        }catch (Exception e){
            e.printStackTrace();
        }

        try {
            pd.put("ksnd", 605);
            pd.put("pinType", "Dukpt");
            pd.put("ksn", call.argument("pinKsn"));
            pd.put("pinBlock", call.argument("cardPIN"));

            Log.d("pd body", String.valueOf(pd));
        }catch (Exception e){
            e.printStackTrace();
        }

        try {
            header.put("transaction", transaction);
            Log.d(String.valueOf(header));
        }catch (Exception e){
            e.printStackTrace();
        }

        JsonObjectRequest JsonObjectR = new JsonObjectRequest
                (Request.Method.POST, url, header, response -> {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            android.util.Log.e("Success", String.valueOf(response));
                            result.success(String.valueOf(response));
                        }
                    });

                }, error -> {
                    // Handle network related Errors
                    if (error.networkResponse == null) {

                        // Handle network Timeout error
                        if (error.getClass().equals(TimeoutError.class)) {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    result.error("2-0-1","An error occured", "Request Timeout Error!");

                                }
                            });
                        } else {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    result.error("2-0-2","An error occured","Network Error. No Internet Connection!");

                                }
                            });
                        }
                    } else {
                        String body;
                        //get status code here
                        final String statusCode = String.valueOf(error.networkResponse.statusCode);
                        //get response body and parse with appropriate encoding
                        try {
                            body = new String(error.networkResponse.data,"UTF-8");
                            android.util.Log.e("Parse error", new Gson().toJson(body));

                        } catch (UnsupportedEncodingException e) {
                            // exception
                        }
                    }
                }) {

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization",  "Bearer " + call.argument("authToken"));
                return headers;
            }
        };

        JsonObjectR.setRetryPolicy(new DefaultRetryPolicy(100000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));


        // Send the JSON request
        queue.add(JsonObjectR);

    }
}
