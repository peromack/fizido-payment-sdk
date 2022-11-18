package com.pos.empressa.nexgo_pos.Blusalt;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.TimeoutError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

public class BlusaltApiService {
    private static String TAG = "Blusalt";

    public static void chargeTransaction(@NonNull MethodChannel.Result result, Context mContext, @NonNull MethodCall call) {
        RequestQueue queue = Volley.newRequestQueue(mContext);

        Log.d(TAG, "Calling charge TRNX");

        String url = "https://dev-wallets.blusalt.net/pos/charge/";

        JSONObject header = new JSONObject();
        try {
            Log.d(TAG, "Calling charge TRNX 2");

            header.put("batteryInformation","100");
            header.put("currencyCode", call.argument("countryCode"));
            header.put("languageInfo", "EN");
            header.put("posConditionCode", "00");
            header.put("printerStatus", "1");
            header.put("ApplicationInterchangeProfile", call.argument("applicationInterchangeProfile"));
            header.put("CvmResults", call.argument("cardholderVerificationResult"));
            header.put("TransactionCurrencyCode", call.argument("transactionCurrencyCode"));
            header.put("TerminalCountryCode",call.argument("countryCode"));
            header.put("TerminalType", call.argument("terminalType"));
            header.put("TransactionType", call.argument("transactionType"));
            header.put("stan", call.argument("originalStan"));
            header.put("minorAmount", "000000000001");
            header.put("ksnd","605" );
            header.put("surcharge", "1057");
            header.put("extendedTransactionType", "6103");
            header.put("posEntryMode", "051");
            header.put("cardSequenceNumber", call.argument("csn"));
            header.put("posDataCode", "510101511344101");
            header.put("posGeoCode", "00234000000000566");
            header.put("atc", call.argument("applicationTransactionCounter"));
            header.put("TerminalVerificationResult", call.argument("terminalVerificationResult"));
            header.put("iad", call.argument("issuerAppData"));
            header.put("TerminalCapabilities", call.argument("terminalCapabilities"));
            header.put("keyLabel", "000002");
            header.put("receivingInstitutionId", "519911");
            header.put("destinationAccountNumber", "5400608971");
            header.put("retrievalReferenceNumber", "000000245866");
            header.put("pinType", "Dukpt");
            header.put("terminalId", call.argument("terminalId"));


            String cardMonthYear = call.argument("cardExpiry");

            header.put("expiryYear", cardMonthYear.substring(0,2));
            header.put("expiryMonth", cardMonthYear.substring(2));

            header.put("pan", call.argument("cardPAN"));
            header.put("track2", call.argument("cardTrack2"));
            header.put("AmountAuthorized", call.argument("transactionAmount"));
            header.put("AmountOther", call.argument("anotherAmount"));
            header.put("TransactionDate", call.argument("transactionDate"));
            header.put("CryptogramInformationData", "80");

            header.put("fromAccount", call.argument("accountType"));
            header.put("ksn", call.argument("pinKsn"));

            String trxCardPin = call.argument("cardPIN");
            if(trxCardPin.length() > 0) {
                header.put("pinBlock", call.argument("cardPIN"));
            }

            header.put("Cryptogram", call.argument("authorizationRequest"));
            header.put("UnpredictableNumber", call.argument("unpredictableNumber"));
            header.put("DedicatedFileName", call.argument("dedicatedFileName"));

            Log.d(TAG, "Charge request body" + header);
        }catch (Exception e){
            e.printStackTrace();
        }

        JsonObjectRequest JsonObjectR = new JsonObjectRequest
                (Request.Method.POST, url, header, response -> {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, new Gson().toJson(response));
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
                            Log.d(TAG, new Gson().toJson(body));

                            Log.d(TAG, new Gson().toJson(error.networkResponse.data));

                        } catch (UnsupportedEncodingException e) {
                            // exception
                        }
                    }
                }) {

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("x-api-key", call.argument("authToken"));
                return headers;
            }
        };

        JsonObjectR.setRetryPolicy(new DefaultRetryPolicy(100000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));


        // Send the JSON request
        queue.add(JsonObjectR);

    }
}
