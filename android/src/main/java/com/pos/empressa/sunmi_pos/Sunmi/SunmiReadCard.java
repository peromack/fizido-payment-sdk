package com.pos.empressa.sunmi_pos.Sunmi;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.nexgo.common.ByteUtils;
import com.pos.empressa.sunmi_pos.Sunmi.utils.ByteUtil;
import com.pos.empressa.sunmi_pos.Sunmi.utils.EmvUtil;
import com.pos.empressa.sunmi_pos.Sunmi.utils.TLV;
import com.pos.empressa.sunmi_pos.Sunmi.utils.TLVUtil;
import com.pos.empressa.sunmi_pos.Sunmi.utils.ThreadPoolUtil;
import com.pos.empressa.sunmi_pos.Sunmi.utils.ToastUtil;
import com.pos.empressa.sunmi_pos.Sunmi.wrapper.CheckCardCallbackV2Wrapper;
import com.pos.empressa.sunmi_pos.ksnUtil.KSNUtilities;
import com.sunmi.pay.hardware.aidl.bean.CardInfo;
import com.sunmi.pay.hardware.aidlv2.AidlConstantsV2;
import com.sunmi.pay.hardware.aidlv2.AidlErrorCodeV2;
import com.sunmi.pay.hardware.aidlv2.bean.EMVCandidateV2;
import com.sunmi.pay.hardware.aidlv2.bean.PinPadConfigV2;
import com.sunmi.pay.hardware.aidlv2.emv.EMVListenerV2;
import com.sunmi.pay.hardware.aidlv2.emv.EMVOptV2;
import com.sunmi.pay.hardware.aidlv2.pinpad.PinPadListenerV2;
import com.sunmi.pay.hardware.aidlv2.pinpad.PinPadOptV2;
import com.sunmi.pay.hardware.aidlv2.readcard.CheckCardCallbackV2;
import com.sunmi.pay.hardware.aidlv2.readcard.ReadCardOptV2;
import com.sunmi.pay.hardware.aidlv2.security.SecurityOptV2;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import io.flutter.plugin.common.MethodChannel;

public class SunmiReadCard extends Application {
    private static final String TAG = "SunmiReadCard";
    private final Context mContext;
    private EMVOptV2 mEMVOptV2;
    private PinPadOptV2 mPinPadOptV2;
    private ReadCardOptV2 mReadCardOptV2;
    private int mCardType;  // card type
    private String mCardNo; // card number
    private int mPinType;   // 0-online pin, 1-offline pin
    private int mAppSelect = 0;
    private ToastUtil toastUtil;
    private Map<String, String> configMap;
    private MethodChannel.Result results;
    private String amount;
    private String isOnline = "1";
    private String pinBlockArray = "";
    private String pinKsn = "";
    private Handler mHandler = new Handler(Looper.getMainLooper());

    public SunmiReadCard(Context mContext) {
        this.mContext = mContext;
        toastUtil = new ToastUtil(mContext);
    }

    public void searchCard(@NonNull MethodChannel.Result result, int transactionAmount) {
        try {
            amount = String.valueOf(transactionAmount);
            results = result;

            initData();
            initEMVAndPinPad();

            startEMVReadCard(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initEMVAndPinPad() {
        mEMVOptV2 = SunmiApplication.app.emvOptV2;
        mPinPadOptV2 = SunmiApplication.app.pinPadOptV2;
        mReadCardOptV2 = SunmiApplication.app.readCardOptV2;
    }

    private void initData() {
        configMap = EmvUtil.getConfig(EmvUtil.COUNTRY_NIGERIA);
        ThreadPoolUtil.executeInCachePool(
                () -> {
                    EmvUtil.initKey(mContext);
                    EmvUtil.setTerminalParam(configMap);
                }
        );
        mHandler.post(() -> {
            toastUtil.toast.showToast("emv init process finished.");
        });
    }

    private void startEMVReadCard(MethodChannel.Result result) {
        try {
            mEMVOptV2.initEmvProcess();
            initEmvTlvData();
            checkCard();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void initEmvTlvData() {
        try {
            // set PayPass(MasterCard) tlv data
            String[] tagsPayPass = {"DF8117", "DF8118", "DF8119", "DF811F", "DF811E", "DF812C",
                    "DF8123", "DF8124", "DF8125", "DF8126",
                    "DF811B", "DF811D", "DF8122", "DF8120", "DF8121"};
            String[] valuesPayPass = {"E0", "F8", "F8", "E8", "00", "00",
                    "000000000000", "000000100000", "999999999999", "000000100000",
                    "30", "02", "0000000000", "000000000000", "000000000000"};
            mEMVOptV2.setTlvList(AidlConstantsV2.EMV.TLVOpCode.OP_PAYPASS, tagsPayPass, valuesPayPass);

            // set AMEX(AmericanExpress) tlv data
            String[] tagsAE = {"9F6D", "9F6E", "9F33", "9F35", "DF8168", "DF8167", "DF8169", "DF8170"};
            String[] valuesAE = {"C0", "D8E00000", "E0E888", "22", "00", "00", "00", "60"};
            mEMVOptV2.setTlvList(AidlConstantsV2.EMV.TLVOpCode.OP_AE, tagsAE, valuesAE);

            String[] tagsJCB = {"9F53", "DF8161"};
            String[] valuesJCB = {"708000", "7F00"};
            mEMVOptV2.setTlvList(AidlConstantsV2.EMV.TLVOpCode.OP_JCB, tagsJCB, valuesJCB);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void checkCard() {
        try {
            mHandler.post(() -> {
                toastUtil.toast.showToast("Please insert card!!!");
            });
            int cardType = AidlConstantsV2.CardType.NFC.getValue() | AidlConstantsV2.CardType.IC.getValue();
            mReadCardOptV2.checkCard(cardType, mCheckCardCallback, 60);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Check card callback
     */
    private final CheckCardCallbackV2 mCheckCardCallback = new CheckCardCallbackV2Wrapper() {
        @Override
        public void findMagCard(Bundle bundle) throws RemoteException {}

        @Override
        public void findICCard(String atr) throws RemoteException {
            Log.d(SunmiReadCard.TAG, "findICCard:" + atr);
            SunmiApplication.app.basicOptV2.buzzerOnDevice(1, 2750, 200, 0);
            mCardType = AidlConstantsV2.CardType.IC.getValue();
            transactProcess();
        }

        @Override
        public void findRFCard(String uuid) throws RemoteException {}

        @Override
        public void onError(int code, String message) throws RemoteException {
            String error = "onError:" + message + " -- " + code;
            Log.d(SunmiReadCard.TAG, error);
            mHandler.post(() -> {
                toastUtil.toast.showToast(error);
            });
        }
    };

    private void transactProcess() {
        Log.e(SunmiReadCard.TAG, "transactProcess");
        try {
            Bundle bundle = new Bundle();
            bundle.putString("amount", amount);
            bundle.putString("transType", "00");
            if (mCardType == AidlConstantsV2.CardType.NFC.getValue()) {
                bundle.putInt("flowType", AidlConstantsV2.EMV.FlowType.TYPE_NFC_SPEEDUP);
            } else {
                bundle.putInt("flowType", AidlConstantsV2.EMV.FlowType.TYPE_EMV_STANDARD);
            }
            bundle.putInt("cardType", mCardType);
            mEMVOptV2.transactProcessEx(bundle, mEMVListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final EMVListenerV2 mEMVListener = new EMVListenerV2.Stub() {
        @Override
        public void onWaitAppSelect(List<EMVCandidateV2> appNameList, boolean isFirstSelect) throws RemoteException {
            Log.e(SunmiReadCard.TAG, "onWaitAppSelect isFirstSelect:" + isFirstSelect);
            String[] candidateNames = getCandidateNames(appNameList);
            try {
                mEMVOptV2.importAppSelect(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onAppFinalSelect(String tag9F06Value) throws RemoteException {
            Log.d(SunmiReadCard.TAG, "onAppFinalSelect tag9F06Value:" + tag9F06Value);
            if (tag9F06Value != null && tag9F06Value.length() > 0) {
                boolean isVerve = tag9F06Value.startsWith("A00000037");
                boolean isUnionPay = tag9F06Value.startsWith("A000000333");
                boolean isVisa = tag9F06Value.startsWith("A000000003");
                boolean isMaster = tag9F06Value.startsWith("A000000004")
                        || tag9F06Value.startsWith("A000000005");
                boolean isAmericanExpress = tag9F06Value.startsWith("A000000025");
                boolean isJCB = tag9F06Value.startsWith("A000000065");
                boolean isRupay = tag9F06Value.startsWith("A000000524");
                boolean isPure = tag9F06Value.startsWith("D999999999")
                        || tag9F06Value.startsWith("D888888888")
                        || tag9F06Value.startsWith("D777777777")
                        || tag9F06Value.startsWith("D666666666")
                        || tag9F06Value.startsWith("A000000615");
                String paymentType = "unknown";
                if (isUnionPay) {
                    paymentType = "UnionPay";
                    mAppSelect = 0;
                } else if (isVisa) {
                    paymentType = "Visa";
                    mAppSelect = 1;
                } else if (isMaster) {
                    paymentType = "MasterCard";
                    mAppSelect = 2;
                } else if (isAmericanExpress) {
                    paymentType = "AmericanExpress";
                } else if (isJCB) {
                    paymentType = "JCB";
                } else if (isRupay) {
                    paymentType = "Rupay";
                } else if (isPure) {
                    paymentType = "Pure";
                } else if (isVerve) {
                    paymentType = "Verve";
                }
                Log.e(SunmiReadCard.TAG, "detect " + paymentType + " card");
            }
            try {
                Log.d(SunmiReadCard.TAG, "importFinalAppSelectStatus status:" + 0);
                mEMVOptV2.importAppFinalSelectStatus(0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfirmCardNo(String cardNo) throws RemoteException {
            Log.d(SunmiReadCard.TAG, "onConfirmCardNo cardNo:" + cardNo);
            mCardNo = cardNo;
            try {
                mEMVOptV2.importCardNoStatus(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onRequestShowPinPad(int pinType, int remainTime) throws RemoteException {
            Log.e(SunmiReadCard.TAG, "onRequestShowPinPad pinType:" + pinType + " remainTime:" + remainTime);
            mPinType = pinType;
            if (mCardNo == null) {
                mCardNo = getCardNo();
            }
            initPinPad();
        }

        @Override
        public void onRequestSignature() throws RemoteException {
            Log.d(SunmiReadCard.TAG, "onRequestSignature");
            importSignatureStatus(0);
        }

        @Override
        public void onCertVerify(int certType, String certInfo) throws RemoteException {
            Log.d(SunmiReadCard.TAG, "onCertVerify certType:" + certType + " certInfo:" + certInfo);
        }

        @Override
        public void onOnlineProc() throws RemoteException {
            Log.d(SunmiReadCard.TAG, "onOnlineProcess");
            try {
                String[] tags = {};
                String[] values = {};
                byte[] out = new byte[1024];
                mEMVOptV2.importOnlineProcStatus(0, tags, values, out);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onCardDataExchangeComplete() throws RemoteException {
            if (mCardType == AidlConstantsV2.CardType.NFC.getValue()) {
                SunmiApplication.app.basicOptV2.buzzerOnDevice(1, 2750, 200, 0);
            }
        }

        @Override
        public void onTransResult(int code, String desc) throws RemoteException {
            if (mCardNo == null) {
                mCardNo = getCardNo();
            }
            Log.d(SunmiReadCard.TAG, "onTransResult code:" + code + " desc:" + desc);
            Log.d(SunmiReadCard.TAG, "***************************************************************");
            Log.d(SunmiReadCard.TAG, "****************************End Process************************");
            Log.d(SunmiReadCard.TAG, "***************************************************************");
            if (code == 0) {
                byte[] outData = new byte[2048];
                int len = mEMVOptV2.getTlvList(AidlConstantsV2.EMV.TLVOpCode.OP_NORMAL, EmvUtil.tags, outData);
                Log.d(SunmiReadCard.TAG, ByteUtil.bytes2HexStr(outData));
                if (len <= 0) {
                    Log.d(SunmiReadCard.TAG, "getCardData error,code:" + len);
                    mHandler.post(() -> {
                        toastUtil.toast.showToast("Error reading card!!!");
                    });
                }

                KSNUtilities ksnUtilitites = new KSNUtilities();
                String workingKey2 = ksnUtilitites.getWorkingKey("3F2216D8297BCE9C", pinKsn);
                Log.d(SunmiReadCard.TAG, "pinKSn 2" + ksnUtilitites.getLatestKsn());

                byte[] bytes = Arrays.copyOf(outData, len);
                HashMap<String, String> cardDataMap = (HashMap<String, String>) TLVUtil.tlvToMap(bytes);
                cardDataMap.put("pan", mCardNo);
                cardDataMap.put("CardPin", pinBlockArray);
                cardDataMap.put("ksn", ksnUtilitites.getLatestKsn());
                cardDataMap.put("isOnline", isOnline);

                Log.d(SunmiReadCard.TAG, String.valueOf(cardDataMap));


                checkAndRemoveCard();
                mHandler.post(() -> {
                    try {
                        results.success(cardDataMap);

                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                    toastUtil.toast.showToast("Success!!!");
                });
            } else if (code == 4) {
//                tryAgain();
            } else {
                mHandler.post(() -> {
                    checkAndRemoveCard();
                    toastUtil.toast.showToast("Card Read Failed!!!");
                });
            }
        }

        @Override
        public void onConfirmationCodeVerified() throws RemoteException {}

        @Override
        public void onRequestDataExchange(String cardNo) throws RemoteException {
            mEMVOptV2.importDataExchangeStatus(0);
        }

        @Override
        public void onTermRiskManagement() throws RemoteException {
            mEMVOptV2.importTermRiskManagementStatus(0);
        }

        @Override
        public void onPreFirstGenAC() throws RemoteException {
            mEMVOptV2.importPreFirstGenACStatus(0);
        }
    };


    private String[] getCandidateNames(List<EMVCandidateV2> candiList) {
        if (candiList == null || candiList.size() == 0) return new String[0];
        String[] result = new String[candiList.size()];
        for (int i = 0; i < candiList.size(); i++) {
            EMVCandidateV2 candi = candiList.get(i);
            String name = candi.appPreName;
            name = TextUtils.isEmpty(name) ? candi.appLabel : name;
            name = TextUtils.isEmpty(name) ? candi.appName : name;
            name = TextUtils.isEmpty(name) ? "" : name;
            result[i] = name;
            Log.d(SunmiReadCard.TAG, "EMVCandidateV2: " + name);
        }
        return result;
    }

    private String getCardNo() {
        Log.d(SunmiReadCard.TAG, "getCardNo");
        try {
            String[] tagList = {"57", "5A"};
            byte[] outData = new byte[256];
            int len = mEMVOptV2.getTlvList(AidlConstantsV2.EMV.TLVOpCode.OP_NORMAL, tagList, outData);
            if (len <= 0) {
                Log.d(SunmiReadCard.TAG, "getCardNo error,code:" + len);
                return "";
            }
            byte[] bytes = Arrays.copyOf(outData, len);
            Map<String, TLV> tlvMap = TLVUtil.buildTLVMap(bytes);
            if (!TextUtils.isEmpty(Objects.requireNonNull(tlvMap.get("57")).getValue())) {
                TLV tlv57 = tlvMap.get("57");
                CardInfo cardInfo = parseTrack2(tlv57.getValue());
                return cardInfo.cardNo;
            }
            if (!TextUtils.isEmpty(Objects.requireNonNull(tlvMap.get("5A")).getValue())) {
                return Objects.requireNonNull(tlvMap.get("5A")).getValue();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static CardInfo parseTrack2(String track2) {
        Log.d(SunmiReadCard.TAG, "track2:" + track2);
        String track_2 = stringFilter(track2);
        int index = track_2.indexOf("=");
        if (index == -1) {
            index = track_2.indexOf("D");
        }
        CardInfo cardInfo = new CardInfo();
        if (index == -1) {
            return cardInfo;
        }
        String cardNumber = "";
        if (track_2.length() > index) {
            cardNumber = track_2.substring(0, index);
        }
        String expiryDate = "";
        if (track_2.length() > index + 5) {
            expiryDate = track_2.substring(index + 1, index + 5);
        }
        String serviceCode = "";
        if (track_2.length() > index + 8) {
            serviceCode = track_2.substring(index + 5, index + 8);
        }
        Log.d(SunmiReadCard.TAG, "cardNumber:" + cardNumber + " expireDate:" + expiryDate + " serviceCode:" + serviceCode);
        cardInfo.cardNo = cardNumber;
        cardInfo.expireDate = expiryDate;
        cardInfo.serviceCode = serviceCode;
        return cardInfo;
    }

    static String stringFilter(String str) {
        String regEx = "[^0-9=D]";
        Pattern p = Pattern.compile(regEx);
        Matcher matcher = p.matcher(str);
        return matcher.replaceAll("").trim();
    }

    private void initPinPad() {
        Log.d(SunmiReadCard.TAG, "initPinPad");
        try {
            PinPadConfigV2 pinPadConfig = new PinPadConfigV2();
            pinPadConfig.setPinPadType(0);
            pinPadConfig.setPinType(mPinType);
            pinPadConfig.setOrderNumKey(false);
            int length = mCardNo.length();
            byte[] panBlock = mCardNo.substring(length - 13, length - 1).getBytes("US-ASCII");
            pinPadConfig.setPan(panBlock);
            pinPadConfig.setTimeout(60 * 1000); // input password timeout
            pinPadConfig.setPinKeyIndex(0);    // pik index
            pinPadConfig.setMaxInput(12);
            pinPadConfig.setMinInput(0);
            pinPadConfig.setKeySystem(1);
            pinPadConfig.setAlgorithmType(0);
//            pinPadConfig.setPinblockFormat(AidlConstants.PinBlockFormat.SEC_PIN_BLK_ISO_FMT4);
            mPinPadOptV2.initPinPad(pinPadConfig, mPinPadListener);

            Log.d(SunmiReadCard.TAG, "Pin pad config " + pinPadConfig);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final PinPadListenerV2 mPinPadListener = new PinPadListenerV2.Stub() {
        @Override
        public void onPinLength(int len) {
            Log.d(SunmiReadCard.TAG, "onPinLength:" + len);
        }

        @Override
        public void onConfirm(int i, byte[] pinBlock) throws RemoteException {
            if (i == 0) {
                isOnline = "0";
            } else {
                isOnline = "1";
            }

            if (pinBlock != null) {
                String hexStr = ByteUtil.bytes2HexStr(pinBlock);
                Log.d(SunmiReadCard.TAG, "onConfirm pin block:" + hexStr);
                SecurityOptV2 securityOptV2 = SunmiApplication.app.securityOptV2;
                pinBlockArray = ByteUtils.byteArray2HexString(pinBlock).toUpperCase();                              //Set the pinBlockArray (String) to the return value 'data' (PIN output) for sending to host

                byte[] dataOut = new byte[10]; //change to 12 if it doesn't
                int result = securityOptV2.dukptCurrentKSN(0, dataOut);
                if (result == 0) {
                    pinKsn = ByteUtil.bytes2HexStr(dataOut).toUpperCase();
                    Log.d(SunmiReadCard.TAG, "onConfirm pin KSN:" + pinKsn);
                    int incrKSNRes = securityOptV2.dukptIncreaseKSN(0);
                    Log.d(SunmiReadCard.TAG, "increasing pin KSN:" + incrKSNRes);
                }

                importPinInputStatus(0);
            } else {
                importPinInputStatus(2);
            }
        }

        @Override
        public void onCancel() {
            Log.d(SunmiReadCard.TAG, "onCancel");
            importPinInputStatus(1);
        }

        @Override
        public void onError(int code) {
            Log.d(SunmiReadCard.TAG, "onError:" + code);
            String msg = AidlErrorCodeV2.valueOf(code).getMsg();
            mHandler.post(() -> {
                toastUtil.toast.showToast("Pin error:" + msg);
                importPinInputStatus(3);
            });
        }
    };

    /**
     * @param inputResult 0:success,1:input PIN canceled,2:input PIN skipped,3:PINPAD problem,4:input PIN timeout
     */
    private void importPinInputStatus(int inputResult) {
        Log.d(SunmiReadCard.TAG, "importPinInputStatus:" + inputResult);
        try {
            mEMVOptV2.importPinInputStatus(mPinType, inputResult);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void importSignatureStatus(int status) {
        Log.d(SunmiReadCard.TAG, "importSignatureStatus status:" + status);
        try {
            mEMVOptV2.importSignatureStatus(status);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkAndRemoveCard() {
        try {
            int status = mReadCardOptV2.getCardExistStatus(mCardType);
            if (status < 0) {
                Log.d(SunmiReadCard.TAG, "getCardExistStatus error, code:" + status);
                return;
            }
            if (status == AidlConstantsV2.CardExistStatus.CARD_ABSENT) {
            } else if (status == AidlConstantsV2.CardExistStatus.CARD_PRESENT) {
                mHandler.post(() -> {
                    toastUtil.toast.showToast("Please remove card!!!");
                });
                SunmiApplication.app.basicOptV2.buzzerOnDevice(1, 2750, 200, 0);
                cancelCheckCard();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cancelCheckCard() {
        try {
            SunmiApplication.app.readCardOptV2.cardOff(AidlConstantsV2.CardType.IC.getValue());
            SunmiApplication.app.readCardOptV2.cancelCheckCard();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
