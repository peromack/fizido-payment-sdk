package com.pos.empressa.horizon_pos.Horizon;

import static com.pos.empressa.horizon_pos.Horizon.pay.TransactionResultCode.APPROVED_BY_ONLINE;
import static com.pos.empressa.horizon_pos.Horizon.pay.TransactionResultCode.DECLINED_BY_ONLINE;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.horizonpay.smartpossdk.aidl.emv.CandidateAID;
import com.horizonpay.smartpossdk.aidl.emv.IAidlEmvL2;
import com.horizonpay.smartpossdk.aidl.pinpad.DukptObj;
import com.horizonpay.smartpossdk.aidl.pinpad.IAidlPinpad;
import com.horizonpay.smartpossdk.data.PinpadConst;
import com.horizonpay.utils.ToastUtils;
import com.pos.empressa.horizon_pos.Horizon.pay.CardReadMode;
import com.pos.empressa.horizon_pos.Horizon.pay.CreditCard;
import com.pos.empressa.horizon_pos.Horizon.pay.OnlineRespEntitiy;
import com.pos.empressa.horizon_pos.Horizon.pay.PayProcessor;
import com.pos.empressa.horizon_pos.Horizon.pay.TransactionResultCode;
import com.pos.empressa.horizon_pos.Horizon.utils.EmvUtil;
import com.pos.empressa.horizon_pos.ksnUtil.KSNUtilities;
import com.socsi.utils.HexUtil;

import java.util.HashMap;
import java.util.List;

import io.flutter.plugin.common.MethodChannel;

public class HorizonReadCard {
    private final String TAG = HorizonReadCard.class.getName();
    Handler handler = new Handler(Looper.getMainLooper());
    private PayProcessor payProcessor;
    IAidlEmvL2 mEmvL2;
    private IAidlPinpad mPinPad;
    private boolean isSupport;
    private String ksn;
    private String pinBlc;
    private String cPin = "";
    private String pan;


    private Context mContext;

    public HorizonReadCard(Context mContext) {
        this.mContext = mContext;
    }

    public void searchCard(@NonNull MethodChannel.Result result, int transactionAmount) {
        try {
            mEmvL2 = DeviceHelper.getEmvHandler();
            mPinPad = DeviceHelper.getPinpad();
            isSupport = mEmvL2.isSupport();

            payProcessor = new PayProcessor(mContext);

            if (!isSupport) {
                ToastHelper("Error: Api not support");
                return;
            }

            ToastHelper("Enter your card");

            payProcessor.pay(transactionAmount, new PayProcessor.PayProcessorListener() {
                @Override
                public void onRetry(int retryFlag) {
                    if (retryFlag == 0) {
                        Log.d(TAG, "Please Insert/Tap Card");
                    } else {
                        Log.d(TAG, ">>>onRetry");
                    }
                }

                @Override
                public void getPinAndKSNData(String pinBlock, String KSN) {
                    ksn = KSN;
                    pinBlc = pinBlock;
                }

                @Override
                public void getCardPin(String cardPin) {
                    getClearPin(cardPin);
                }

                @Override
                public void cardNumber(String cardNo) {
                    pan = cardNo;
                }

                @Override
                public void onCardDetected(final CardReadMode cardReadMode, CreditCard creditCard) {
                    Log.d(">>>onCardDetected: ", cardReadMode.toString());

                    switch (cardReadMode) {
                        case SWIPE:
                            pan = creditCard.getCardNumber();
                            StringBuilder builder = new StringBuilder();
                            builder.append("-----------\nCard: " + creditCard.getCardNumber());
                            builder.append("\nExpiry Date: " + creditCard.getExpireDate());
                            builder.append("\nCardholderName: " + creditCard.getHolderName());
                            builder.append("\nCardSequenceNumber: " + creditCard.getCardSequenceNumber());
                            builder.append("\nTrack1: " + creditCard.getMagData().getTrack1());
                            builder.append("\nTrack2: " + creditCard.getMagData().getTrack2());
                            builder.append("-------------------\n");
                            Log.d(TAG, builder.toString());
                            payProcessor.magCardInputPIN(creditCard.getCardNumber());

                            break;
                        case CONTACT:
                        case CONTACTLESS:
                            break;

                    }
                }


                @Override
                public CandidateAID confirmApplicationSelection(List<CandidateAID> candidateList) {
//                    int selectedIndex = 0;
//                    try {
//                        selectedIndex = new AppSelectDialog(mContext, candidateList).call();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                    Log.d(TAG, ">>>confirmApplicationSelection:" + candidateList.get(selectedIndex).getAid());
//                    return candidateList.get(selectedIndex);
                    return null;
                }

                @Override
                public OnlineRespEntitiy onPerformOnlineProcessing(CreditCard creditCard) {
                    Log.d(TAG, ">>>onPerformOnlineProcessing:");

                    return null;
                }

                @Override
                public void onCompleted(TransactionResultCode res, CreditCard creditCard) {
                    final StringBuilder resultText = new StringBuilder();
                    switch (res) {
                        case APPROVED_BY_OFFLINE:
                            resultText.append(TransactionResultCode.APPROVED_BY_OFFLINE.toString());
                            break;
                        case APPROVED_BY_ONLINE:
                            resultText.append(APPROVED_BY_ONLINE.toString());
                            break;
                        case DECLINED_BY_OFFLINE:
                            resultText.append(TransactionResultCode.DECLINED_BY_OFFLINE.toString());
                            break;
                        case DECLINED_BY_ONLINE:
                            resultText.append(DECLINED_BY_ONLINE.toString());
                            break;
                        case DECLINED_BY_TERMINAL_NEED_REVERSE:
                            resultText.append(TransactionResultCode.DECLINED_BY_TERMINAL_NEED_REVERSE.toString());
                            break;
                        case ERROR_TRANSCATION_CANCEL:
                            resultText.append(TransactionResultCode.ERROR_TRANSCATION_CANCEL.toString());
                            break;
                        case ERROR_UNKNOWN:
                            resultText.append(TransactionResultCode.ERROR_UNKNOWN.toString());
                            break;
                    }

                    KSNUtilities ksnUtilitites = new KSNUtilities();
                    String workingKey = ksnUtilitites.getWorkingKey("3F2216D8297BCE9C", getInitialKSN());
                    String pinBlock =  ksnUtilitites.DesEncryptDukpt(workingKey, pan, cPin);

                    HashMap<String, String> cardDataMap = EmvUtil.showEmvTransResult();

                    ksn = ksnUtilitites.getLatestKsn();

                    cardDataMap.put("ksn", ksn);
                    cardDataMap.put("CardPin", pinBlock);

                    Log.d("pin from credit card", creditCard.getPIN());

                    Log.d(TAG, ">>>onCompleted :" + cardDataMap + "\n" + "..............");

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                result.success(cardDataMap);

                            } catch (NullPointerException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    stopEmvProcess();
                }
            });


        } catch (Exception e) {
            e.printStackTrace();
            stopEmvProcess();
        }
    }

    private String getInitialKSN(){
        SharedPreferences sharedPref = mContext.getSharedPreferences( "KSNCOUNTER", Context.MODE_PRIVATE);
        int ksn = sharedPref.getInt("KSN",00001);
        if(ksn > 9999){
            ksn = 00000 ;
        }
        int latestKSN = ksn + 1 ;
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("KSN",latestKSN);
        editor.apply();
        return  "0000000002DDDDE"+ String.format("%05d", latestKSN);
    }

    private void dukptInit() {
        try {
            String key = "C1D0F8FB4958670DBA40AB1F3752EF0D";
            String ksn = "FFFF9876543210" + "000000";

            mPinPad.setKeyAlgorithm(PinpadConst.KeyAlgorithm.DUKPT);

            DukptObj dukptObj = new DukptObj(key, ksn, PinpadConst.DukptKeyType.DUKPT_IPEK_PLAINTEXT, PinpadConst.DukptKeyIndex.DUKPT_KEY_INDEX_0);
            int ret = mPinPad.dukptKeyLoad(dukptObj);


        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void ToastHelper(String text) {
        handler.post(
                () -> Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show()
        );
    }

    private void getClearPin(String data) {
        char[] ary = data.toCharArray();
        StringBuilder cardPins = new StringBuilder();
        for (int i = 0; i < ary.length; i++)
        {
            if (i % 2 == 1)
            {
                cardPins.append(ary[i]);
            }
        }

        String result =  cardPins.toString();
        cPin = result;
        Log.d("result card ", result);
    }

    private void increaseKsn() {

        try {
            String ksn = mPinPad.dukptKsnIncrease(PinpadConst.DukptKeyIndex.DUKPT_KEY_INDEX_0);
        } catch (RemoteException e) {
            e.printStackTrace();
        }


    }

    private String getCurrentKsn() {
        String ksn = null;
        try {
             ksn = mPinPad.dukptCurrentKsn(PinpadConst.DukptKeyIndex.DUKPT_KEY_INDEX_0);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return ksn;
    }

    public void stopEmvProcess() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mEmvL2 != null) {
                        mEmvL2.stopEmvProcess();
                    }
                } catch (RemoteException | NullPointerException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
