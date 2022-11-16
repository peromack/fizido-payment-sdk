package com.pos.empressa.nexgo_pos.Horizon.pay;


import static com.pos.empressa.nexgo_pos.Horizon.pay.TransactionResultCode.ERROR_TRANSCATION_CANCEL;
import static com.pos.empressa.nexgo_pos.Horizon.pay.TransactionResultCode.ERROR_TRANSCATION_TIMEOUT;
import static com.pos.empressa.nexgo_pos.Horizon.pay.TransactionResultCode.ERROR_UNKNOWN;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;

import com.horizonpay.smartpossdk.aidl.cardreader.IAidlCardReader;
import com.horizonpay.smartpossdk.aidl.emv.AidlCheckCardListener;
import com.horizonpay.smartpossdk.aidl.emv.AidlEmvStartListener;
import com.horizonpay.smartpossdk.aidl.emv.CandidateAID;
import com.horizonpay.smartpossdk.aidl.emv.EmvFinalSelectData;
import com.horizonpay.smartpossdk.aidl.emv.EmvTags;
import com.horizonpay.smartpossdk.aidl.emv.EmvTransData;
import com.horizonpay.smartpossdk.aidl.emv.EmvTransOutputData;
import com.horizonpay.smartpossdk.aidl.emv.IAidlEmvL2;
import com.horizonpay.smartpossdk.aidl.magcard.TrackData;
import com.horizonpay.smartpossdk.aidl.pinpad.AidlPinPadInputListener;
import com.horizonpay.smartpossdk.aidl.pinpad.IAidlPinpad;
import com.horizonpay.smartpossdk.data.EmvConstant;
import com.horizonpay.smartpossdk.data.PinpadConst;

import com.horizonpay.utils.ConvertUtils;
import com.pos.empressa.nexgo_pos.BuildConfig;
import com.pos.empressa.nexgo_pos.Horizon.DeviceHelper;
import android.util.Log;
import com.pos.empressa.nexgo_pos.Horizon.utils.EmvUtil;
import com.pos.empressa.nexgo_pos.Horizon.utils.HexUtil;
import com.pos.empressa.nexgo_pos.Horizon.utils.TlvData;
import com.pos.empressa.nexgo_pos.Horizon.utils.TlvDataList;

import java.util.ArrayList;
import java.util.List;


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
public class PayProcessor {
    public interface PayProcessorListener {
        void onRetry(int retryFlag);

        void getPinAndKSNData(String pinBlock, String KSN);

        void getCardPin(String cardPin);

        void cardNumber(String cardNo);

        void onCardDetected(CardReadMode cardReadMode, CreditCard creditCard);

        CandidateAID confirmApplicationSelection(List<CandidateAID> candidateList);

        OnlineRespEntitiy onPerformOnlineProcessing(CreditCard creditCard);

        void onCompleted(TransactionResultCode result, CreditCard creditCard);

    }

    private IAidlCardReader mCardReader;
    private IAidlEmvL2 mEmvL2;
    private IAidlPinpad mPinPad;
    private PayProcessorListener mListener;
    private long mAount;
    private CardReadMode mCardReadMode = CardReadMode.MANUAL;
    private OnlineRespEntitiy mOnlineRespEntitiy;
    private long startTick;
    private final String LOG_TAG = PayProcessor.class.getSimpleName();
    private Context mContext;
    private CreditCard creditCard;

    public PayProcessor(Context context) {
        mContext = context;
    }

    public PayProcessor() {
        try {
            mCardReader = DeviceHelper.getCardReader();
            mEmvL2 = DeviceHelper.getEmvHandler();
            mPinPad = DeviceHelper.getPinpad();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    private void init() {
        mOnlineRespEntitiy = null;
        mCardReadMode = CardReadMode.MANUAL;

    }

    public void pay(long amount, PayProcessorListener listener) {
        init();
        mListener = listener;
        mAount = amount;
        try {
            mListener.onRetry(0);
            startTick = System.currentTimeMillis();
            creditCard = new CreditCard();
            mCardReader.searchCard(true, true, true, 100, checkCardListener);
        } catch (RemoteException e) {
            mListener.onCompleted(ERROR_UNKNOWN, new CreditCard());
            e.printStackTrace();
        }
    }

    private AidlCheckCardListener.Stub checkCardListener = new AidlCheckCardListener.Stub() {
        @Override
        public void onFindMagCard(TrackData data) throws RemoteException {
            Log.d(LOG_TAG, "card NO:" + data.getCardNo());
            mCardReadMode = CardReadMode.SWIPE;
            creditCard.setCardReadMode(CardReadMode.SWIPE);
            creditCard.setCardNumber(data.getCardNo());
            creditCard.setExpireDate(data.getExpiryDate());
            creditCard.setHolderName(data.getCardholderName());
            creditCard.setServiceCode(data.getServiceCode());
            CreditCard.MagData magData = new CreditCard.MagData(data.getTrack1Data(), data.getTrack2Data());
            creditCard.setMagData(magData);
            mListener.onCardDetected(CardReadMode.SWIPE, creditCard);
        }

        @Override
        public void onSwipeCardFail() throws RemoteException {
            TransactionResultCode transactionResultCode = ERROR_UNKNOWN;
            mListener.onCompleted(transactionResultCode, new CreditCard());

        }

        @Override
        public void onFindICCard() throws RemoteException {
            mCardReadMode = CardReadMode.CONTACT;
            Log.i(LOG_TAG, "onFindICCard: ");
            Log.i(LOG_TAG, "time = " + (System.currentTimeMillis() - startTick) + "ms");
            creditCard.setCardReadMode(CardReadMode.CONTACT);
            mListener.onCardDetected(CardReadMode.CONTACT, creditCard);
            stopSearch();
            Log.i(LOG_TAG, "startEMVProcess>>>>>: ");
            startEMVProcess();
        }

        @Override
        public void onFindRFCard(int ctlsCardType) throws RemoteException {
            mCardReadMode = CardReadMode.CONTACTLESS;
            Log.d(LOG_TAG, "onFindRFCard: ");
            Log.i(LOG_TAG, "time = " + (System.currentTimeMillis() - startTick) + "ms");
            creditCard.setCardReadMode(CardReadMode.CONTACTLESS);
            mListener.onCardDetected(CardReadMode.CONTACTLESS, creditCard);
            stopSearch();
            startEMVProcess();
        }

        @Override
        public void onTimeout() throws RemoteException {
            Log.d(LOG_TAG, "SearchCard = onTimeout ");
            mListener.onCompleted(ERROR_TRANSCATION_TIMEOUT, new CreditCard());
        }

        @Override
        public void onCancelled() throws RemoteException {
            Log.d(LOG_TAG, "SearchCard = onCancelled ");
            mListener.onCompleted(ERROR_UNKNOWN, new CreditCard());

        }

        @Override
        public void onError(int errCode) throws RemoteException {
            Log.e(LOG_TAG, "SearchCard = onError " + errCode);
            mListener.onCompleted(ERROR_UNKNOWN, new CreditCard());

        }
    };

    private AidlEmvStartListener.Stub emvStartListener = new AidlEmvStartListener.Stub() {
        @Override
        public void onRequestAmount() throws RemoteException {
            Log.d(LOG_TAG, "emvStartListener onRequestAmount: ");
            mEmvL2.requestAmountResp(String.valueOf(mAount));
        }

        @Override
        public void onRequestAidSelect(int times, List<CandidateAID> aids) throws RemoteException {
//            CandidateAID confirmApplicationSelection = mListener.confirmApplicationSelection(aids);
//            int index = aids.indexOf(confirmApplicationSelection);
//            mEmvL2.requestAidSelectResp(index);
            Log.d(LOG_TAG, "onRequestAidSelect: ");
            selApp(aids);
        }


        @Override
        public void onFinalSelectAid(EmvFinalSelectData emvFinalSelectData) throws RemoteException {
            Log.d(LOG_TAG, "onFinalSelectAid: " + emvFinalSelectData.getAid());
            mEmvL2.requestFinalSelectAidResp("");
        }

        @Override
        public void onConfirmCardNo(final String cardNo) throws RemoteException {
            Log.d(LOG_TAG, "onConfirmCardNo: " + cardNo);
            Log.i(LOG_TAG, "time = " + (System.currentTimeMillis() - startTick) + "ms");
            mListener.cardNumber(cardNo);
            mEmvL2.confirmCardNoResp(true);
        }


        @Override
        public void onRequestPin(boolean isOnlinePIN, int leftTimes) throws RemoteException {
            Log.d(LOG_TAG, "onCardHolderInputPin isOnlinePin: " + isOnlinePIN + "offlinePIN leftTimes: " + leftTimes);

            String PAN = mEmvL2.getTagValue("5A");
            if (PAN != null) {
                PAN = PAN.replace("F", "");
            }
            Log.d(LOG_TAG, "onRequestPin: PAN " + PAN);
            mListener.cardNumber(PAN);
            if (isOnlinePIN) {
                inputOnlinePIN(PAN);
            } else {
                Log.d(LOG_TAG, "onRequestPin: offline PIN");
                inputOfflinePIN(PAN, leftTimes);
            }
        }

        @Override
        public void onResquestOfflinePinDisp(int i) throws RemoteException {
            if (i == 0) {
                Log.d(LOG_TAG, "onResquestOfflinePinDisp: PIN OK !");
            } else {
                Log.d(LOG_TAG, "WRONG PIN --> " + i + "Chance Left");
            }

        }


        @Override
        public void onRequestOnline(EmvTransOutputData emvTransOutputData) throws RemoteException {
            getEmvCardInfo();
            String respCode = "05";
            String iccData = "";
            Log.d(LOG_TAG, "onRequestOnline PIN : " + creditCard.getPIN());
            Log.d(LOG_TAG, "onRequestOnline: true");
            Log.d(LOG_TAG, "time = " + (System.currentTimeMillis() - startTick) + "ms");
            if (BuildConfig.DEBUG == true) {
                Log.d(LOG_TAG, "onRequestOnline: Simulate Online process>>>>");
                iccData = onlineProc();
            } else {
                mOnlineRespEntitiy = mListener.onPerformOnlineProcessing(creditCard);
            }

            if (mOnlineRespEntitiy != null) {
                respCode = mOnlineRespEntitiy.getRespCode();
                iccData = mOnlineRespEntitiy.getIccData();
            }
            Log.d(LOG_TAG, "resp Code: " + respCode);
            Log.d(LOG_TAG, "iccData: " + iccData);
            mEmvL2.requestOnlineResp(respCode, iccData);
        }

        @Override
        public void onFinish(final int emvResult, EmvTransOutputData emvTransOutputData) throws RemoteException {
            Log.d(LOG_TAG, "CallBack onFinish: ");
            Log.d(LOG_TAG, "time = " + (System.currentTimeMillis() - startTick) + "ms");

            emvFinish(emvResult, emvTransOutputData);
        }

        @Override
        public void onError(int errCode) throws RemoteException {
            Log.e(LOG_TAG, "onError: errcode: " + errCode);
            emvFinish(EmvConstant.EmvTransResultCode.ERROR_UNKNOWN, new EmvTransOutputData());
        }


    };

    public String getEmvRecordTLV() {
        final String[] standard_Tags = {
                "9f26",
                "9f27",
                "9f10",
                "9f37",
                "9f36",
                "95",
                "9a",
                "9c",
                "9f02",
                "5f2a",
                "9f1a",
                "82",
                "9f33",
                "9f34",
                "9f03",
                "84",
                "9F08",
                "9f09",
                "9f35",
                "9f1e",
                "9F53",
                "9f41",
                "9f63",
                "9F6E",
                "9F4C",
                "9F5D",
                "9B",
                "5F34",
                "50",
                "9F12",
                "91",
                "DF31",
                "8F"
        };
        try {
            return mEmvL2.getTlvByTags(standard_Tags);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void getEmvCardInfo() {
        creditCard.setCardReadMode(mCardReadMode);
        try {
            String cardsn = mEmvL2.getTagValue(EmvTags.EMV_TAG_IC_PANSN);
            if (cardsn != null && !cardsn.isEmpty()) {
                creditCard.setCardSequenceNumber(cardsn);
            }

            String track2 = mEmvL2.getTagValue(EmvTags.EMV_TAG_IC_TRACK2DATA);
            if (track2 == null || track2.isEmpty()) {
                track2 = mEmvL2.getTagValue(EmvTags.M_TAG_IC_9F6B);
            }
            if (track2 != null && track2.length() > 20) {
                if (track2.endsWith("F") || track2.endsWith("f")) {
                    track2 = track2.substring(0, track2.length() - 1);
                }
                String formatTrack2 = track2.toUpperCase().replace('=', 'D');

                int idx = formatTrack2.indexOf('D');
                String expDate = track2.substring(idx + 1, idx + 5);

                creditCard.setExpireDate(expDate);

                String pan = track2.substring(0, idx);
                creditCard.setCardNumber(pan);
                CreditCard.EmvData emvData = new CreditCard.EmvData("", formatTrack2, getEmvRecordTLV());
                creditCard.setEmvData(emvData);
            }

            String name = EmvUtil.readCardHolder();
            creditCard.setHolderName(ConvertUtils.formatHexString(name));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private String onlineProc() throws RemoteException {
        StringBuilder builder = new StringBuilder();
        String arqcTlv = mEmvL2.getTlvByTags(EmvUtil.arqcTLVTags);
        builder.append(arqcTlv);
        Log.d(LOG_TAG, "onlineProc: arqcTlv: " + arqcTlv);
        if (!TlvDataList.fromBinary(arqcTlv).contains(EmvTags.EMV_TAG_IC_CID)) {
            builder.append(TlvData.fromData(EmvTags.EMV_TAG_IC_CID, new byte[]{(byte) 0x80}));
        }

        TlvDataList tlvDataList = TlvDataList.fromBinary(builder.toString());
        arqcTlv = tlvDataList.toString();


        String apn = mEmvL2.getTagValue(EmvTags.EMV_TAG_IC_APNAME);
        String appLabel = mEmvL2.getTagValue(EmvTags.EMV_TAG_IC_APPLABEL);
        String resultTlv = arqcTlv + TlvData.fromData("AC", TlvDataList.fromBinary(arqcTlv).getTLV(EmvTags.EMV_TAG_IC_AC).getBytesValue())
                + TlvData.fromData(EmvTags.EMV_TAG_TM_TSI, new byte[2])
                + TlvData.fromData(EmvTags.EMV_TAG_TM_CVMRESULT, new byte[3])
                + (TextUtils.isEmpty(appLabel) ? "" : TlvData.fromData(EmvTags.EMV_TAG_IC_APPLABEL, HexUtil.hexStringToByte(appLabel)))
                + (TextUtils.isEmpty(apn) ? "" : TlvData.fromData(EmvTags.EMV_TAG_IC_APNAME, HexUtil.hexStringToByte(apn)));

        Log.d(LOG_TAG, "onlineProc: applable:" + appLabel);
        Bundle online = new Bundle();
        //TODO onlineRespCode is DE 39—RESPONSE CODE, detail see ISO8583
        String onlineRespCode = "00";
        //TODO DE 55.
        byte[] arpcData = EmvUtil.getExampleARPCData();

        if (arpcData == null) {
            return "";
        }

//        online.putString(EmvOnlineResult.REJCODE, onlineRespCode);
//        online.putByteArray(EmvOnlineResult.RECVARPC_DATA, arpcData);
//
        Log.d(LOG_TAG, "result:" + resultTlv);
        return HexUtil.bytesToHexString(arpcData);
    }

    private void emvFinish(int emvResult, EmvTransOutputData emvTransOutputData) throws RemoteException {
        TransactionResultCode transactionResultCode = TransactionResultCode.DECLINED_BY_ONLINE;
        Log.d(LOG_TAG, "emvFinish Result: " + emvResult);
        Log.d(LOG_TAG, "emvFinish AC: " + emvTransOutputData.getAcType());
        Log.d(LOG_TAG, "emvFinish PIN : " + creditCard.getPIN());
        mListener.getCardPin(creditCard.getPIN());
        if (emvResult == EmvConstant.EmvTransResultCode.SUCCESS) { // SUCCESS
            switch (emvTransOutputData.getAcType()) {
                case EmvConstant.EmvACType.AAC: //Trans End
                    if (mOnlineRespEntitiy == null) {
                        transactionResultCode = TransactionResultCode.DECLINED_BY_OFFLINE;
                    } else if ("00".equals(mOnlineRespEntitiy.getRespCode())) {
                        transactionResultCode = TransactionResultCode.DECLINED_BY_TERMINAL_NEED_REVERSE;
                    } else {
                        transactionResultCode = TransactionResultCode.DECLINED_BY_ONLINE;
                    }
                    break;

                case EmvConstant.EmvACType.TC:  //Trans accept
                    if (mOnlineRespEntitiy == null) {
                        transactionResultCode = TransactionResultCode.APPROVED_BY_OFFLINE;
                    } else {
                        transactionResultCode = TransactionResultCode.APPROVED_BY_ONLINE;
                    }
                    break;

                case EmvConstant.EmvACType.ARQC: //ARQC
                    Log.d(LOG_TAG, "onFinish: ARQC");
                    mOnlineRespEntitiy = mListener.onPerformOnlineProcessing(creditCard);
                    if (mOnlineRespEntitiy != null && "00".equals(mOnlineRespEntitiy.respCode)) {
                        transactionResultCode = TransactionResultCode.APPROVED_BY_ONLINE;
                    } else {
                        transactionResultCode = TransactionResultCode.DECLINED_BY_OFFLINE;

                    }
                    break;
            }
            Log.d(LOG_TAG, "emvFinish: " + transactionResultCode.toString());
        } else if (emvResult == EmvConstant.EmvTransResultCode.EMV_RESULT_NOAPP) {  // fallback
            mCardReader.searchCard(true, false, false, 30, new AidlCheckCardListener.Stub() {
                @Override
                public void onFindMagCard(TrackData trackData) throws RemoteException {
                    Log.d(LOG_TAG, "card NO:" + trackData.getCardNo());
                    mCardReadMode = CardReadMode.SWIPE;
                    CreditCard creditCard = new CreditCard();
                    creditCard.setCardReadMode(CardReadMode.SWIPE);
                    creditCard.setCardNumber(trackData.getCardNo());
                    creditCard.setExpireDate(trackData.getExpiryDate());
                    creditCard.setServiceCode(trackData.getServiceCode());
                    CreditCard.MagData magData = new CreditCard.MagData(trackData.getTrack1Data(), trackData.getTrack2Data());
                    creditCard.setMagData(magData);
                    mListener.onCardDetected(CardReadMode.SWIPE, creditCard);

                    StringBuilder builder = new StringBuilder();
                    builder.append("Card: " + trackData.getCardNo());
                    builder.append("\nTk1: " + trackData.getTrack1Data());
                    builder.append("\nTk2: " + trackData.getTrack2Data());
                    builder.append("\nTk3: " + trackData.getTrack3Data());
                    builder.append("\ntrackKSN: " + trackData.getKsn());
                    builder.append("\nExpiryDate: " + trackData.getExpiryDate());
                    builder.append("\nCardholderName: " + trackData.getCardholderName());
                    Log.d(LOG_TAG, "FallBack onFindMagCard: " + builder.toString());
                    stopSearch();
                    magCardInputPIN(creditCard.getCardNumber());
                    mOnlineRespEntitiy = mListener.onPerformOnlineProcessing(creditCard);
//            TransactionResultCode transactionResultCode = TransactionResultCode.DECLINED_BY_OFFLINE;
                    TransactionResultCode transactionResultCode = TransactionResultCode.APPROVED_BY_ONLINE;
                    if (mOnlineRespEntitiy != null && "00".equals(mOnlineRespEntitiy.respCode)) {
                        transactionResultCode = TransactionResultCode.APPROVED_BY_ONLINE;
                    } else {
                        transactionResultCode = TransactionResultCode.DECLINED_BY_OFFLINE;

                    }
                    mListener.onCompleted(transactionResultCode, creditCard);
                }

                @Override
                public void onSwipeCardFail() throws RemoteException {

                }

                @Override
                public void onFindICCard() throws RemoteException {

                }

                @Override
                public void onFindRFCard(int i) throws RemoteException {

                }

                @Override
                public void onTimeout() throws RemoteException {
                    Log.d(LOG_TAG, "fallback onTimeout: ");
                }

                @Override
                public void onCancelled() throws RemoteException {
                    Log.d(LOG_TAG, "fallback onCancelled: ");
                }

                @Override
                public void onError(int i) throws RemoteException {
                    Log.d(LOG_TAG, "fallback onError: " + i);
                }
            });
        } else if (emvResult == EmvConstant.EmvTransResultCode.EMV_RESULT_STOP) { //Cancel
            Log.d(LOG_TAG, "emvFinish: EMV_RESULT_STOP");
            transactionResultCode = ERROR_TRANSCATION_CANCEL;
        } else {

            Log.d(LOG_TAG, "emvFinish: Other result");
            if (mOnlineRespEntitiy != null && "00".equals(mOnlineRespEntitiy.getRespCode())) {
                transactionResultCode = TransactionResultCode.DECLINED_BY_TERMINAL_NEED_REVERSE;
            } else {
                transactionResultCode = ERROR_UNKNOWN;
            }

        }
        mListener.onCompleted(transactionResultCode, creditCard);
    }

    private Bundle setPinpadUI(boolean isOnline) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(PinpadConst.PinpadShow.COMMON_NEW_LAYOUT, true);
        bundle.putString(PinpadConst.PinpadShow.COMMON_OK_TEXT, "Enter");
        bundle.putBoolean(PinpadConst.PinpadShow.COMMON_SUPPORT_BYPASS, false);
        bundle.putBoolean(PinpadConst.PinpadShow.COMMON_SUPPORT_KEYVOICE, false);
        bundle.putBoolean(PinpadConst.PinpadShow.COMMON_IS_RANDOM, true);
        if (isOnline) {
            bundle.putString(PinpadConst.PinpadShow.TITLE_HEAD_CONTENT, "Please Enter PIN");
        } else {
            bundle.putString(PinpadConst.PinpadShow.TITLE_HEAD_CONTENT, "Please Enter Offline PIN");
        }

        return bundle;
    }

    public void magCardInputPIN(String sPAN) {
        Bundle bundle = setPinpadUI(true);
        try {
            mPinPad.inputOnlinePin(bundle, new int[]{4, 6}, 300, sPAN, 0, PinpadConst.PinAlgorithmMode.ISO9564FMT1, new AidlPinPadInputListener.Stub() {
                @Override
                public void onConfirm(byte[] data, boolean noPin, String s) throws RemoteException {

                    Log.d(LOG_TAG, "PIN input:" + (noPin == true ? "NO" : "Yes"));
                    Log.d(LOG_TAG, "PIN Block:" + HexUtil.bytesToHexString(data));
                    // go for online processing.
                    mOnlineRespEntitiy = mListener.onPerformOnlineProcessing(creditCard);
                }

                @Override
                public void onSendKey(int keyCode) throws RemoteException {
                    Log.d(LOG_TAG, "onSendKey:" + keyCode);
                }

                @Override
                public void onCancel() throws RemoteException {
                    Log.d(LOG_TAG, "onCancel: ");
                    mListener.onCompleted(ERROR_TRANSCATION_CANCEL, creditCard);
                }

                @Override
                public void onError(int errorCode) throws RemoteException {
                    Log.e(LOG_TAG, "onError: code:" + errorCode);
                    mListener.onCompleted(ERROR_UNKNOWN, creditCard);
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void inputOnlinePIN(String sPAN) {
        Bundle bundle = setPinpadUI(true);
        bundle.putString(PinpadConst.PinpadShow.TITLE_HEAD_CONTENT, "Please input the online pin");

        try {
            mPinPad.inputOfflinePin(bundle, new int[]{4}, 30, new AidlPinPadInputListener.Stub() {                @Override
                public void onConfirm(byte[] data, boolean noPin, String s) throws RemoteException {
                    StringBuilder builder = new StringBuilder();
                    builder.append("\ntime = " + (System.currentTimeMillis() - startTick) + "ms");
                    builder.append("\nPIN input:" + (noPin == true ? "NO" : "Yes"));
                    builder.append("\nPIN Block:" + HexUtil.bytesToHexString(data));
                    builder.append("\nksn: " + s);
                    Log.d(LOG_TAG, builder.toString());
                    mEmvL2.requestPinResp(data, noPin);
                    creditCard.setPIN(HexUtil.bytesToHexString(data));
                    mListener.getPinAndKSNData(HexUtil.bytesToHexString(data), s);
                }

                @Override
                public void onSendKey(int keyCode) throws RemoteException {
                    Log.d(LOG_TAG, "onSendKey:" + keyCode);
                }

                @Override
                public void onCancel() throws RemoteException {
                    Log.d(LOG_TAG, "inputOnlinePin onCancel: ");
                    mEmvL2.requestPinResp(null, false);
                }

                @Override
                public void onError(int errorCode) throws RemoteException {
                    Log.d(LOG_TAG, "onError: code:" + errorCode);
                    mEmvL2.requestPinResp(null, false);
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void inputOfflinePIN(String pan, int lefttimes) {
        Log.d(LOG_TAG, "inputOfflinePIN: " + pan);
        Bundle bundle = new Bundle();
        bundle.putBoolean(PinpadConst.PinpadShow.COMMON_NEW_LAYOUT, true);
        bundle.putString(PinpadConst.PinpadShow.COMMON_OK_TEXT, "OK");
        bundle.putBoolean(PinpadConst.PinpadShow.COMMON_SUPPORT_BYPASS, false);
        bundle.putBoolean(PinpadConst.PinpadShow.COMMON_SUPPORT_KEYVOICE, false);
        bundle.putBoolean(PinpadConst.PinpadShow.COMMON_IS_RANDOM, true);
        if (lefttimes == 3) {
            bundle.putString(PinpadConst.PinpadShow.TITLE_HEAD_CONTENT, "Please Enter PIN:");
        } else if (lefttimes == 2) {
            bundle.putString(PinpadConst.PinpadShow.TITLE_HEAD_CONTENT, "Please Enter PIN:(2 Chances Left)");
        } else if (lefttimes == 1) {
            bundle.putString(PinpadConst.PinpadShow.TITLE_HEAD_CONTENT, "Please Enter PIN(Last Chance):");
        }

        try {
            mPinPad.inputOfflinePin(bundle, new int[]{4}, 30, new AidlPinPadInputListener.Stub() {
                @Override
                public void onConfirm(byte[] data, boolean noPin, String s) throws RemoteException {
                    StringBuilder builder = new StringBuilder();
                    builder.append("\ntime = " + (System.currentTimeMillis() - startTick) + "ms");
                    builder.append("\nPIN input:" + (noPin == true ? "NO" : "Yes"));
                    builder.append("\nPIN Block:" + HexUtil.bytesToHexString(data));
                    builder.append("\nksn: " + s);
                    mEmvL2.requestPinResp(data, noPin);
                    creditCard.setPIN(ConvertUtils.bytes2HexString(data));
                    mListener.getPinAndKSNData(HexUtil.bytesToHexString(data), s);
                }

                @Override
                public void onSendKey(int keyCode) throws RemoteException {
                    Log.d(LOG_TAG, "onSendKey: " + keyCode);
                }

                @Override
                public void onCancel() throws RemoteException {
                    Log.d(LOG_TAG, "inputOfflinePIN onCancel: ");
                    mEmvL2.requestPinResp(null, false);
                }

                @Override
                public void onError(int errorCode) throws RemoteException {
                    Log.e(LOG_TAG, "onError: code:" + errorCode);
                    mEmvL2.requestPinResp(null, false);
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    private void stopSearch() {
        try {
            mCardReader.cancelSearchCard();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void initEmvParam() {
        try {
            mEmvL2.setTermConfig(EmvUtil.getInitTermConfig());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private <T> ArrayList<T> createArrayList(T... elements) {
        ArrayList<T> list = new ArrayList<T>();
        for (T element : elements) {
            list.add(element);
        }
        return list;
    }

    private void setEmvTransDataExt() {
        Bundle config = new Bundle();
        config.putInt(EmvConstant.EmvTransDataConstants.KERNEL_MODE, 0x01);
        config.putByte(EmvConstant.EmvTransDataConstants.TRANS_TYPE, (byte) 0x00);

        config.putStringArrayList(EmvConstant.EmvTerminalConstraints.CONFIG, createArrayList("DF81180170", "DF81190118", "DF811B0130"));
        try {
            mEmvL2.setTransDataConfigExt(config);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    private void startEMVProcess() {
        initEmvParam();
        EmvTransData emvTransData = new EmvTransData();
        emvTransData.setAmount(mAount);
        emvTransData.setOtherAmount(0);
        emvTransData.setForceOnline(true);
        emvTransData.setEmvFlowType(EmvConstant.EmvTransFlow.FULL);
        setEmvTransDataExt();   // Set ext trans data.
        try {
            mEmvL2.startEmvProcess(emvTransData, emvStartListener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    private void selApp(List<CandidateAID> appList) {
        String[] options = new String[appList.size()];
        for (int i = 0; i < appList.size(); i++) {
            options[i] = appList.get(i).getAppLabel();
        }
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(mContext);
        alertBuilder.setTitle("Please select app");
        alertBuilder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int index) {
                try {
                    mEmvL2.requestAidSelectResp(index);
                } catch (RemoteException e) {

                }
            }
        });
        AlertDialog alertDialog1 = alertBuilder.create();
        alertDialog1.show();
    }

}
