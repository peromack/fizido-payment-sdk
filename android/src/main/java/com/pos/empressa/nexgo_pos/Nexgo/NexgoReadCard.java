package com.pos.empressa.nexgo_pos.Nexgo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.nexgo.common.ByteUtils;
import com.nexgo.common.LogUtils;
import com.nexgo.oaf.apiv3.APIProxy;
import com.nexgo.oaf.apiv3.DeviceEngine;
import com.nexgo.oaf.apiv3.SdkResult;
import com.nexgo.oaf.apiv3.device.pinpad.AlgorithmModeEnum;
import com.nexgo.oaf.apiv3.device.pinpad.DukptKeyTypeEnum;
import com.nexgo.oaf.apiv3.device.pinpad.OnPinPadInputListener;
import com.nexgo.oaf.apiv3.device.pinpad.PinAlgorithmModeEnum;
import com.nexgo.oaf.apiv3.device.pinpad.PinKeyboardModeEnum;
import com.nexgo.oaf.apiv3.device.pinpad.PinPad;
import com.nexgo.oaf.apiv3.device.pinpad.PinPadKeyCode;
import com.nexgo.oaf.apiv3.device.reader.CardInfoEntity;
import com.nexgo.oaf.apiv3.device.reader.CardReader;
import com.nexgo.oaf.apiv3.device.reader.CardSlotTypeEnum;
import com.nexgo.oaf.apiv3.device.reader.OnCardInfoListener;
import com.nexgo.oaf.apiv3.emv.AmexTransDataEntity;
import com.nexgo.oaf.apiv3.emv.CandidateAppInfoEntity;
import com.nexgo.oaf.apiv3.emv.DynamicReaderLimitEntity;
import com.nexgo.oaf.apiv3.emv.EmvDataSourceEnum;
import com.nexgo.oaf.apiv3.emv.EmvEntryModeEnum;
import com.nexgo.oaf.apiv3.emv.EmvHandler2;
import com.nexgo.oaf.apiv3.emv.EmvOnlineResultEntity;
import com.nexgo.oaf.apiv3.emv.EmvProcessFlowEnum;
import com.nexgo.oaf.apiv3.emv.EmvProcessResultEntity;
import com.nexgo.oaf.apiv3.emv.EmvTransConfigurationEntity;
import com.nexgo.oaf.apiv3.emv.OnEmvProcessListener2;
import com.nexgo.oaf.apiv3.emv.PromptEnum;
import com.nexgo.oaf.apiv3.emv.UnionPayTransDataEntity;
import com.pos.empressa.nexgo_pos.Nexgo.utils.EmvUtils;
import com.pos.empressa.nexgo_pos.Nexgo.utils.TlvUtil;
import com.pos.empressa.nexgo_pos.R;
import com.pos.empressa.nexgo_pos.ksnUtil.KSNUtilities;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import io.flutter.plugin.common.MethodChannel;

public class NexgoReadCard extends AppCompatActivity {
    private final int charge = 1075;
    private DeviceEngine deviceEngine;
    private EmvHandler2 emvHandler2;
    private String cardNo;
    private Context mContext;

    private final Activity mActivity;
    private EmvUtils emvUtils;

    private PinPad pinPad;

    private String pwdText = "";
    private TextView pwdTv;
    private AlertDialog pwdAlertDialog;

    String amount;

    String cardPin = "";

    private String isOnline = "1";

    private String pinBlockArray = "";
    private String pinKsn = "";

    MethodChannel.Result results;

    private CardSlotTypeEnum mExistSlot;
    private boolean flag = true;

    private boolean isExpressPaySeePhoneTapCardAgain = false;

    private int fallBackRetries = 0;

    public NexgoReadCard(Context mContext, Activity activity) {
        this.mContext = mContext;
        this.mActivity = activity;
    }

    public void searchCard(@NonNull MethodChannel.Result result, int transactionAmount) {
        try {

            if (deviceEngine == null) {
                deviceEngine = APIProxy.getDeviceEngine(mContext);
            }

            pinPad = deviceEngine.getPinPad();

            emvHandler2 = deviceEngine.getEmvHandler2("app2");

            // enable below lines to capture the EMV logs
            emvHandler2.emvDebugLog(true);
            LogUtils.setDebugEnable(true);

            emvUtils = new EmvUtils(mContext);

            initEmvAidAndCapk();

            amount = String.valueOf(transactionAmount);

            results = result;

            startEmvTest(result);

        } catch (Exception e) {
            e.printStackTrace();
            emvHandler2.emvProcessCancel();
        }
    }

    public void cancelSearch() {
        if (emvHandler2 != null) {
            emvHandler2.emvProcessCancel();
        }
    }

    private void startEmvTest(MethodChannel.Result result) {
        CardReader cardReader = deviceEngine.getCardReader();
        HashSet<CardSlotTypeEnum> slotTypes = new HashSet<>();
        slotTypes.add(CardSlotTypeEnum.ICC1);
        slotTypes.add(CardSlotTypeEnum.RF);

        Toast.makeText(mContext, "Please Insert Card.", Toast.LENGTH_SHORT).show();

        cardReader.searchCard(slotTypes, 60, new OnCardInfoListener() {

            @Override
            public void onCardInfo(int retCode, CardInfoEntity cardInfo) {
                if (retCode == SdkResult.Success && cardInfo != null) {
                    mExistSlot = cardInfo.getCardExistslot();
                    Log.d("nexgo", "====mExistSlot" + mExistSlot.toString());
                    EmvTransConfigurationEntity transData = new EmvTransConfigurationEntity();

                    transData.setTransAmount(amount);
                    // transData.setCashbackAmount("000000000100"); //if support cashback amount
                    transData.setEmvTransType((byte) 0x00); // 0x00-sale, 0x20-refund,0x09-sale with cashback
                    transData.setCountryCode("566"); // CountryCode
                    transData.setCurrencyCode("566"); // CurrencyCode, 566 indicate Nigerian Naira
                    transData.setTermId("00000001");
                    transData.setMerId("000000000000001");
                    transData.setTransDate(new SimpleDateFormat("yyMMdd", Locale.getDefault()).format(new Date()));
                    transData.setTransTime(new SimpleDateFormat("hhmmss", Locale.getDefault()).format(new Date()));
                    transData.setTraceNo("00000000");

                    transData.setEmvProcessFlowEnum(EmvProcessFlowEnum.EMV_PROCESS_FLOW_STANDARD);
                    if (cardInfo.getCardExistslot() == CardSlotTypeEnum.RF) {
                        transData.setEmvEntryModeEnum(EmvEntryModeEnum.EMV_ENTRY_MODE_CONTACTLESS);
                    } else {
                        transData.setEmvEntryModeEnum(EmvEntryModeEnum.EMV_ENTRY_MODE_CONTACT);
                    }

                    setPaywaveDrl();

                    setExpressPayDrl();

                    if (isExpressPaySeePhoneTapCardAgain) {
                        AmexTransDataEntity amexTransDataEntity = new AmexTransDataEntity();
                        amexTransDataEntity.setExpressPaySeePhoneTapCardAgain(true);
                    }

                    // for UPI
                    UnionPayTransDataEntity unionPayTransDataEntity = new UnionPayTransDataEntity();
                    unionPayTransDataEntity.setQpbocForGlobal(true);
                    unionPayTransDataEntity.setSupportCDCVM(true);
                    // if support QPS, please enable below lines
                    // unionPayTransDataEntity.setSupportContactlessQps(true);
                    // unionPayTransDataEntity.setContactlessQpsLimit("000000030000");
                    transData.setUnionPayTransDataEntity(unionPayTransDataEntity);

                    // if you want set contactless aid for first select, you can enable below lines.
                    // it is only used for contactless
                    // for example, the card have paypass and pure application(paypass priority is
                    // highest), but the local bank required use pure application,
                    // in this situation , you can use below method.
                    // emvHandler2.contactlessSetAidFirstSelect((byte) 0x07,
                    // ByteUtils.hexString2ByteArray("a0000000041010"));
                    // emvHandler2.contactlessSetAidFirstSelect((byte) 0x07,
                    // ByteUtils.hexString2ByteArray("a0000001524010"));

                    Log.d("nexgo", "start emv ");
                    readcard(result, transData);
                } else {

                    Log.d("Search Card Error", "Search Card Failed");
                }
            }

            @Override
            public void onSwipeIncorrect() {
                Log.d("Search Card", "please search card again");
            }

            @Override
            public void onMultipleCards() {
                // cardReader.stopSearch(); //before next search card, please stopSearch first

                Log.d("Search Card", "please tap one card");
            }
        });
        // Toast.makeText(mContext, "please insert or tap card",
        // Toast.LENGTH_SHORT).show();

    }

    private void readcard(@NonNull MethodChannel.Result result, EmvTransConfigurationEntity transData) {
        Log.d("Read Card", "Calling Read Card");
        emvHandler2.emvProcess(transData, new OnEmvProcessListener2() {
            @Override
            public void onSelApp(final List<String> appNameList, List<CandidateAppInfoEntity> appInfoList,
                    boolean isFirstSelect) {
                Log.d("Read Card", "Calling Select App");
            }

            @Override
            public void onTransInitBeforeGPO() {
                Log.d("nexgo", "onAfterFinalSelectedApp");
                byte[] aid = emvHandler2.getTlv(new byte[] { 0x4F }, EmvDataSourceEnum.FROM_KERNEL);

                if (mExistSlot == CardSlotTypeEnum.RF) {
                    if (aid != null) {
                        if (ByteUtils.byteArray2HexString(aid).toUpperCase().contains("A000000004")) {
                            // Paypass
                            configPaypassParameter(aid);
                        } else if (ByteUtils.byteArray2HexString(aid).toUpperCase().contains("A000000003")) {
                            // Paywave
                            configPaywaveParameters();
                        } else if (ByteUtils.byteArray2HexString(aid).toUpperCase().contains("A000000025")) {
                            // ExpressPay
                            configExpressPayParameter();
                        } else if (ByteUtils.byteArray2HexString(aid).toUpperCase().contains("A000000541")) {
                            // configPureContactlessParameter();
                        } else if (ByteUtils.byteArray2HexString(aid).toUpperCase().contains("A000000065")) {
                            // configJcbContactlessParameter();
                        }
                    }
                } else {
                    // contact terminal capability ; if different card brand(depend on aid) have
                    // different terminal capability
                    // if(ByteUtils.byteArray2HexString(aid).toUpperCase().contains("A000000004")){
                    // emvHandler2.setTlv(new byte[]{(byte)0x9F,(byte)0x33}, new
                    // byte[]{(byte)0xE0,(byte)0xF8,(byte)0xC8});
                    // emvHandler2.setTlv(new byte[]{(byte)0x9F,(byte)0x1D},
                    // ByteUtils.hexString2ByteArray("6C00800000000000"));//terminal risk

                    // }
                }

                emvHandler2.onSetTransInitBeforeGPOResponse(true);
            }

            @Override
            public void onContactlessTapCardAgain() {
                Log.d("nexgo", "onReadCardAgain");

                // this method only used for EMV contactless card if the host response the
                // script. Such as paywave , AMEX...

                // for paywave,
                // onOnlineProc-->onSetOnlineProcResponse->onContactlessTapCardAgain--> search
                // contactless card ->onReadCardAgainResponse->onFinish

                // emvHandler.onSetReadCardAgainResponse(true);
            }

            @Override
            public void onConfirmCardNo(final CardInfoEntity cardInfo) {
                Log.d("nexgo", "onConfirmCardNo" + new Gson().toJson(cardInfo));
                Log.d("nexgo", "onConfirmCardNo" + cardInfo.getTk2());
                Log.d("nexgo", "onConfirmCardNo" + cardInfo.getCardNo());
                if (mExistSlot == CardSlotTypeEnum.RF) {
                    emvHandler2.onSetConfirmCardNoResponse(true);
                    return;
                }

                cardNo = cardInfo.getCardNo();
                emvHandler2.onSetConfirmCardNoResponse(true);
            }

            @Override
            public void onCardHolderInputPin(final boolean isOnlinePin, int leftTimes) {
                Log.d("nexgo", "onCardHolderInputPin isOnlinePin = " + isOnlinePin);
                Log.d("nexgo", "onCardHolderInputPin leftTimes = " + leftTimes);

                runOnUiThread(() -> showInputPin(isOnlinePin, leftTimes));
            }

            @Override
            public void onRemoveCard() {
                Log.d("nexgo", "onRemoveCard");

                emvHandler2.onSetRemoveCardResponse();
            }

            @Override
            public void onPrompt(PromptEnum promptEnum) {
                Log.d("nexgo", "onPrompt->" + promptEnum);
                emvHandler2.onSetPromptResponse(true);
            }

            @Override
            public void onOnlineProc() {
                Log.d("nexgo", "onOnlineProc");

                Log.d("nexgo", "getEmvContactlessMode:" + emvHandler2.getEmvContactlessMode());
                Log.d("nexgo", "getcardinfo:" + new Gson().toJson(emvHandler2.getEmvCardDataInfo()));
                Log.d("nexgo", "getEmvCvmResult:" + emvHandler2.getEmvCvmResult());
                Log.d("nexgo", "getSignNeed--" + emvHandler2.getSignNeed());

                byte[] tlv_5A = emvHandler2.getTlv(new byte[] { (byte) 0x5A }, EmvDataSourceEnum.FROM_KERNEL);
                Log.d("nexgo", "tlv_5A--" + ByteUtils.byteArray2HexString(tlv_5A));

                byte[] tlv_95 = emvHandler2.getTlv(new byte[] { (byte) 0x95 }, EmvDataSourceEnum.FROM_KERNEL);
                Log.d("nexgo", "tlv_95--" + ByteUtils.byteArray2HexString(tlv_95));

                byte[] tlv_84 = emvHandler2.getTlv(new byte[] { (byte) 0x84 }, EmvDataSourceEnum.FROM_KERNEL);
                Log.d("nexgo", "tlv_84--" + ByteUtils.byteArray2HexString(tlv_84));

                byte[] tlv_50 = emvHandler2.getTlv(new byte[] { (byte) 0x50 }, EmvDataSourceEnum.FROM_KERNEL);
                Log.d("nexgo", "tlv_50--" + ByteUtils.byteArray2HexString(tlv_50));

                EmvOnlineResultEntity emvOnlineResult = new EmvOnlineResultEntity();
                emvOnlineResult.setAuthCode("123450");
                emvOnlineResult.setRejCode("00");
                // fill with the host response 55 field EMV data to do second auth, the format
                // should be TLV format.
                // for example: 910870741219600860008a023030 91 = tag, 08 = len,
                // 7074121960086000 = value;
                // 8a = tag, 02 = len, 3030 = value
                emvOnlineResult.setRecvField55(null);
                emvHandler2.onSetOnlineProcResponse(SdkResult.Success, emvOnlineResult);

            }

            @Override
            public void onFinish(final int retCode, EmvProcessResultEntity entity) {
                Log.d("nexgo", "onFinish" + "retCode :" + retCode); // -8014
                if (pwdAlertDialog != null) {
                    pwdAlertDialog.dismiss();
                }

                boolean flag = false;
                byte[] aid = emvHandler2.getTlv(new byte[] { 0x4F }, EmvDataSourceEnum.FROM_KERNEL);
                if (aid != null) {
                    if (mExistSlot == CardSlotTypeEnum.RF) {
                        if (ByteUtils.byteArray2HexString(aid).toUpperCase().contains("A000000025")) {
                            if (retCode == SdkResult.Emv_Plz_See_Phone) {
                                isExpressPaySeePhoneTapCardAgain = true;
                                flag = true;
                            }
                        }
                    }
                }
                if (!flag) {
                    isExpressPaySeePhoneTapCardAgain = false;
                }

                String tlvData = emvHandler2.getTlvByTags(EmvUtils.tags);

                Log.d("nexgo", "tlvData:" + tlvData);

                // Building Card data Map
                HashMap<String, String> cardDataMap = (HashMap<String, String>) TlvUtil.tlvToMap(tlvData);

                KSNUtilities ksnUtilitites = new KSNUtilities();
                String workingKey2 = ksnUtilitites.getWorkingKey("3F2216D8297BCE9C", pinKsn);
                Log.d(NexgoReadCard.class.getName(), "pinKSn 2" + ksnUtilitites.getLatestKsn());

                cardDataMap.put("pan", cardNo);
                cardDataMap.put("CardPin", pinBlockArray);
                cardDataMap.put("ksn", ksnUtilitites.getLatestKsn());
                cardDataMap.put("isOnline", isOnline);

                Log.d(NexgoReadCard.class.getName(), ">>>onCompleted :" + cardDataMap + "\n" + ".............." + "\n" +
                        "pinKSn " + pinKsn + "\n" + "pinBlockArray " + pinBlockArray);

                // get CVM result
                Log.d("nexgo", "getEmvCvmResult:" + emvHandler2.getEmvCvmResult());

                Log.d("nexgo", "emvHandler2.getSignNeed()--" + emvHandler2.getSignNeed());

                // get card number, track 2 data...etc
                Log.d("nexgo", "getcardinfo:" + new Gson().toJson(emvHandler2.getEmvCardDataInfo()));

                byte[] tlv_5A = emvHandler2.getTlv(new byte[] { (byte) 0x5A }, EmvDataSourceEnum.FROM_KERNEL);
                Log.d("nexgo", "tlv_5A--" + ByteUtils.byteArray2HexString(tlv_5A));

                byte[] tlv_95 = emvHandler2.getTlv(new byte[] { (byte) 0x95 }, EmvDataSourceEnum.FROM_KERNEL);
                Log.d("nexgo", "tlv_95--" + ByteUtils.byteArray2HexString(tlv_95));

                byte[] tlv_84 = emvHandler2.getTlv(new byte[] { (byte) 0x84 }, EmvDataSourceEnum.FROM_KERNEL);
                Log.d("nexgo", "tlv_84--" + ByteUtils.byteArray2HexString(tlv_84));

                byte[] tlv_50 = emvHandler2.getTlv(new byte[] { (byte) 0x50 }, EmvDataSourceEnum.FROM_KERNEL);
                Log.d("nexgo", "tlv_50--" + ByteUtils.byteArray2HexString(tlv_50));

                switch (retCode) {
                    case SdkResult.Emv_Success_Arpc_Fail:
                    case SdkResult.Success:
                        Log.d("nexgo", "success");
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    fallBackRetries = 0;
                                    result.success(cardDataMap);
                                } catch (NullPointerException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        break;
                    case SdkResult.Emv_Script_Fail:
                        // online approve
                        break;

                    case SdkResult.Emv_Qpboc_Offline:// EMV Contactless: Offline Approval
                    case SdkResult.Emv_Offline_Accept:// EMV Contact: Offline Approval
                        // offline approve
                        break;

                    // this retcode is Abolished
                    case SdkResult.Emv_Qpboc_Online:// EMV Contactless: Online Process for union pay
                        // union pay online contactless--application should go online
                        break;

                    case SdkResult.Emv_Candidatelist_Empty:// Application have no aid list
                    case SdkResult.Emv_FallBack:// FallBack ,chip card reset failed
                        // fallback process
                        // TODO(Read card again)
                        Log.d("nexgo", "fallback retrying");

                        if (fallBackRetries < 2) {
                            readcard(result, transData);
                            fallBackRetries += 1;
                        }
                        break;

                    case SdkResult.Emv_Arpc_Fail: //
                    case SdkResult.Emv_Declined:
                        // online decline ,if it is in second gac, application should decide if it is
                        // need reversal the transaction
                        break;

                    case SdkResult.Emv_Cancel:// Transaction Cancel
                        // user cancel
                        break;

                    case SdkResult.Emv_Offline_Declined: //
                        // offline decline
                        break;

                    case SdkResult.Emv_Card_Block: // Card Block
                        // card is blocked
                        break;

                    case SdkResult.Emv_App_Block: // Application Block
                        // card application block
                        break;

                    case SdkResult.Emv_App_Ineffect:
                        // card not active
                        break;

                    case SdkResult.Emv_App_Expired:
                        // card Expired
                        break;

                    case SdkResult.Emv_Other_Interface:
                        // try other entry mode, like contact or mag-stripe
                        break;

                    case SdkResult.Emv_Plz_See_Phone:
                        // see phone flow
                        // prompt a dialog to user to check phone-->search contactless card(another
                        // card) -->start new emvProcess again
                        break;

                    case SdkResult.Emv_Terminate:
                        // transaction terminate
                        break;

                    default:
                        // other error
                        break;
                }
                emvHandler2.emvProcessCancel();
            }
        });

    }

    private void setExpressPayDrl() {
        DynamicReaderLimitEntity defaultDynamicReaderLimitEntity = new DynamicReaderLimitEntity();
        defaultDynamicReaderLimitEntity.setAppProgID(new byte[] { (byte) 0xFF });
        defaultDynamicReaderLimitEntity.setReaderCVMReqLimit(ByteUtils.hexString2ByteArray("000000001000"));
        defaultDynamicReaderLimitEntity.setReaderContactlessTransLimit(ByteUtils.hexString2ByteArray("000000001500"));
        defaultDynamicReaderLimitEntity.setReaderContactlessFloorLimit(ByteUtils.hexString2ByteArray("000000001200"));

        List<DynamicReaderLimitEntity> dynamicReaderLimitEntities = new ArrayList<>();
        DynamicReaderLimitEntity dynamicReaderLimitEntity = new DynamicReaderLimitEntity();
        dynamicReaderLimitEntity.setAppProgID(new byte[] { (byte) 0x06 });
        dynamicReaderLimitEntity.setReaderCVMReqLimit(ByteUtils.hexString2ByteArray("000000000200"));
        dynamicReaderLimitEntity.setReaderContactlessTransLimit(ByteUtils.hexString2ByteArray("000000000700"));
        dynamicReaderLimitEntity.setReaderContactlessFloorLimit(ByteUtils.hexString2ByteArray("000000000400"));
        dynamicReaderLimitEntities.add(dynamicReaderLimitEntity);

        dynamicReaderLimitEntity = new DynamicReaderLimitEntity();
        dynamicReaderLimitEntity.setAppProgID(new byte[] { (byte) 0x0B });
        dynamicReaderLimitEntity.setReaderCVMReqLimit(ByteUtils.hexString2ByteArray("000000000200"));
        dynamicReaderLimitEntity.setReaderContactlessTransLimit(ByteUtils.hexString2ByteArray("000000000300"));
        dynamicReaderLimitEntity.setReaderContactlessFloorLimit(ByteUtils.hexString2ByteArray("000000000100"));
        dynamicReaderLimitEntities.add(dynamicReaderLimitEntity);

        emvHandler2.setDynamicReaderLimitListForExpressPay(defaultDynamicReaderLimitEntity, dynamicReaderLimitEntities);
    }

    private void setPaywaveDrl() {
        List<DynamicReaderLimitEntity> dynamicReaderLimitEntity = new ArrayList<>();

        DynamicReaderLimitEntity entity = new DynamicReaderLimitEntity();
        entity.setDrlSupport(true);
        entity.setAppProgID(new byte[] { 0x31, 0x02, 0x68, 0x26, 0x20 });// get from 9f5a
        entity.setAuthOfZeroCheck(true);
        entity.setStatusCheck(false);
        entity.setReaderCVMReqLimitCheck(true);
        entity.setReaderContactlessFloorLimitCheck(true);
        entity.setReaderContactlessTransLimitCheck(false);
        entity.setReaderCVMReqLimit(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x50, 0x01 });
        entity.setReaderContactlessFloorLimit(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x50, 0x00 });
        entity.setReaderContactlessTransLimit(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x60, 0x01 });
        dynamicReaderLimitEntity.add(entity);

        DynamicReaderLimitEntity entity1 = new DynamicReaderLimitEntity();
        entity1.setDrlSupport(true);
        entity1.setAppProgID(new byte[] { 0x31, 0x02, 0x68, 0x26, 0x12, 0x00, 0x00, 0x03 });// get from 9f5a
        entity1.setStatusCheck(false);
        entity1.setAuthOfZeroCheck(true);
        entity1.setReaderCVMReqLimitCheck(true);
        entity1.setReaderContactlessFloorLimitCheck(true);
        entity1.setReaderContactlessTransLimitCheck(false);
        entity1.setReaderCVMReqLimit(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x50, 0x01 });
        entity1.setReaderContactlessFloorLimit(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x50, 0x00 });
        entity1.setReaderContactlessTransLimit(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x60, 0x01 });
        dynamicReaderLimitEntity.add(entity1);

        DynamicReaderLimitEntity entity2 = new DynamicReaderLimitEntity();
        entity2.setDrlSupport(true);
        entity2.setAppProgID(new byte[] { 0x31, 0x02, 0x68, 0x26, 0x12 });// get from 9f5a
        entity2.setAuthOfZeroCheck(true);
        entity2.setStatusCheck(false);
        entity2.setReaderCVMReqLimitCheck(true);
        entity2.setReaderContactlessFloorLimitCheck(true);
        entity2.setReaderContactlessTransLimitCheck(false);
        entity2.setReaderCVMReqLimit(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x50, 0x01 });
        entity2.setReaderContactlessFloorLimit(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x50, 0x00 });
        entity2.setReaderContactlessTransLimit(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x60, 0x01 });
        dynamicReaderLimitEntity.add(entity2);

        DynamicReaderLimitEntity entity3 = new DynamicReaderLimitEntity();
        entity3.setDrlSupport(true);
        entity3.setAppProgID(new byte[] { 0x31, 0x02, 0x68, 0x26, 0x00 });// get from 9f5a
        entity3.setAuthOfZeroCheck(true);
        entity3.setStatusCheck(false);
        entity3.setReaderCVMReqLimitCheck(true);
        entity3.setReaderContactlessFloorLimitCheck(true);
        entity3.setReaderContactlessTransLimitCheck(false);
        entity3.setReaderCVMReqLimit(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x50, 0x01 });
        entity3.setReaderContactlessFloorLimit(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x50, 0x00 });
        entity3.setReaderContactlessTransLimit(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x60, 0x01 });
        dynamicReaderLimitEntity.add(entity3);

        emvHandler2.setDynamicReaderLimitListForPaywave(dynamicReaderLimitEntity);
    }

    private void configPaywaveParameters() {
        byte[] TTQ;
        byte[] kernelTTQ = emvHandler2.getTlv(ByteUtils.hexString2ByteArray("9F66"), EmvDataSourceEnum.FROM_KERNEL);
        Log.d("nexgo", "configPaywaveParameters, TTQ" + ByteUtils.byteArray2HexString(kernelTTQ));
        // default TTQ value
        TTQ = ByteUtils.hexString2ByteArray("36004000");
        kernelTTQ[0] = TTQ[0];
        kernelTTQ[2] = TTQ[2];
        kernelTTQ[3] = TTQ[3];

        emvHandler2.setTlv(ByteUtils.hexString2ByteArray("9F66"), kernelTTQ);
    }

    private void configPaypassParameter(byte[] aid) {
        // kernel configuration, enable RRP and cdcvm
        emvHandler2.setTlv(new byte[] { (byte) 0xDF, (byte) 0x81, (byte) 0x1B }, new byte[] { (byte) 0x30 });

        // EMV MODE :amount >contactless cvm limit, set 60 = online pin and signature
        emvHandler2.setTlv(new byte[] { (byte) 0xDF, (byte) 0x81, (byte) 0x18 }, new byte[] { (byte) 0x60 });
        // EMV mode :amount < contactless cvm limit, set 08 = no cvm
        emvHandler2.setTlv(new byte[] { (byte) 0xDF, (byte) 0x81, (byte) 0x19 }, new byte[] { (byte) 0x08 });

        emvHandler2.setTlv(new byte[] { (byte) 0xDF, (byte) 0x81, (byte) 0x25 },
                ByteUtils.hexString2ByteArray("000999999999"));

        if (ByteUtils.byteArray2HexString(aid).toUpperCase().contains("A0000000043060")) {
            Log.d("nexgo", "======maestro===== ");
            // maestro only support online pin
            emvHandler2.setTlv(new byte[] { (byte) 0x9F, (byte) 0x33 },
                    new byte[] { (byte) 0xE0, (byte) 0x40, (byte) 0xC8 });
            emvHandler2.setTlv(new byte[] { (byte) 0xDF, (byte) 0x81, (byte) 0x18 }, new byte[] { (byte) 0x40 });
            emvHandler2.setTlv(new byte[] { (byte) 0xDF, (byte) 0x81, (byte) 0x19 }, new byte[] { (byte) 0x08 });

            // set 9F1D terminal risk management - Maestro. it should be same with the MTIP
            // configuration for 9F1D
            emvHandler2.setTlv(new byte[] { (byte) 0x9F, (byte) 0x1d },
                    ByteUtils.hexString2ByteArray("4C00800000000000"));
        } else {
            // set 9F1D terminal risk management - MasterCard. it should be same with the
            // MTIP configuration for 9F1D
            emvHandler2.setTlv(new byte[] { (byte) 0x9F, (byte) 0x1d },
                    ByteUtils.hexString2ByteArray("6C00800000000000"));
        }

    }

    private void configExpressPayParameter() {
        // set terminal capability...
        byte[] TTC;
        byte[] kernelTTC = emvHandler2.getTlv(ByteUtils.hexString2ByteArray("9F6E"), EmvDataSourceEnum.FROM_KERNEL);

        TTC = ByteUtils.hexString2ByteArray("D8C00000");
        kernelTTC[1] = TTC[1];

        emvHandler2.setTlv(ByteUtils.hexString2ByteArray("9F6E"), kernelTTC);

        //
        // //TacDefault
        // emvHandler2.setTlv(ByteUtils.hexString2ByteArray("DF8120"),
        // ByteUtils.hexString2ByteArray("fc50b8a000"));
        //
        // //TacDecline
        // emvHandler2.setTlv(ByteUtils.hexString2ByteArray("DF8121"),
        // ByteUtils.hexString2ByteArray("0000000000"));
        //
        // //TacOnline
        // emvHandler2.setTlv(ByteUtils.hexString2ByteArray("DF8122"),
        // ByteUtils.hexString2ByteArray("fc50808800"));
    }

    private void configPureContactlessParameter() {
        Log.d("nexgo", "configPureContactlessParameter");
        // emvHandler2.setPureKernelCapab(ByteUtils.hexString2ByteArray("3400400A99"));

    }

    private void configJcbContactlessParameter() {
        Log.d("nexgo", "configJcbContactlessParameter");

    }

    private byte[] pinRes = new byte[8];;

    private void showInputPin(boolean isOnlinPin, int leftTimes) {
        int INJECTED_PIN_SLOT = 0;
        if (pinPad.dukptCurrentKsn(INJECTED_PIN_SLOT) == null) {
            // There is no key injected; cannot continue - show some error to user and break
            // out
            Log.e("Nexgo", "startInputPin() : cannot continue, No key is injected!");
            emvHandler2.emvProcessCancel(); // Stop the EMV process, cannot proceed to enter PIN without the injected
                                            // key
            return;
        }
        int[] pinLen = new int[] { 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c };
        pinPad.setPinKeyboardMode(PinKeyboardModeEnum.RANDOM);
        // Set the PINPAD algorithm mode - we want to use DUKPT
        pinPad.setAlgorithmMode(AlgorithmModeEnum.DUKPT);
        byte[] panBytes = ByteUtils.string2ASCIIByteArray(cardNo);

        LayoutInflater inflater = LayoutInflater.from(mActivity);
        View dv = inflater.inflate(R.layout.pin_key_layout, null);

        pwdAlertDialog = new AlertDialog.Builder(mActivity, R.style.CustomAlertDialogStyle).setView(dv).create();
        pwdTv = dv.findViewById(R.id.pin_tv);

        TextView amount = dv.findViewById(R.id.amount_tv);
        amount.setText(formatAmount(amountForDisplay()));
        if (!isOnlinPin) {
            TextView triesLeft = dv.findViewById(R.id.tries_left_tv);
            triesLeft.setText(String.format("%d tries left", leftTimes));
            triesLeft.setVisibility(View.VISIBLE);
        }
        pwdAlertDialog.setCanceledOnTouchOutside(false);
        pwdAlertDialog.show();

        OnPinPadInputListener pinPadInputListener = new OnPinPadInputListener() {
            @Override
            public void onInputResult(int retCode, byte[] data) {
                pwdText = "";
                if (pwdAlertDialog != null) {
                    pwdAlertDialog.dismiss();
                }
                if (retCode == SdkResult.Success || retCode == SdkResult.PinPad_No_Pin_Input
                        || retCode == SdkResult.PinPad_Input_Cancel) {
                    if (data != null) {
                        if (isOnlinPin) {
                            byte[] temp = new byte[8];
                            System.arraycopy(data, 0, temp, 0, 8);

                            pinBlockArray = ByteUtils.byteArray2HexString(data).toUpperCase(); // Set the pinBlockArray
                                                                                               // (String) to the return
                                                                                               // value 'data' (PIN
                                                                                               // output) for sending to
                                                                                               // host
                            pinKsn = ByteUtils.byteArray2HexString(pinPad.dukptCurrentKsn(0)); // Save the pinKsn in
                                                                                               // case needed to send to
                                                                                               // host

                            char lastChar = pinKsn.charAt(pinKsn.length() - 1);
                            if (lastChar == '9') {
                                injectKSN(pinPad);
                            } else {
                                pinPad.dukptKsnIncrease(0); // Incremenent the KSN counter
                            }
                        }
                    } else {
                        Log.d("CArd pin result", "is empty");
                    }
                    emvHandler2.onSetPinInputResponse(retCode != SdkResult.PinPad_Input_Cancel,
                            retCode == SdkResult.PinPad_No_Pin_Input);
                } else {
                    Log.d("nexgo", "pin enter failed");
                    emvHandler2.onSetPinInputResponse(false, false);
                }
            }

            @Override
            public void onSendKey(byte b) {
                runOnUiThread(() -> {
                    if (b == PinPadKeyCode.KEYCODE_CLEAR) {
                        pwdText = "";
                    } else {
                        pwdText += "* ";
                    }
                    pwdTv.setText(pwdText);
                });
            }
        };
        if (isOnlinPin) {
            isOnline = "0";
            if (cardNo == null) {
                cardNo = emvHandler2.getEmvCardDataInfo().getCardNo();
            }
            pinPad.inputOnlinePin(pinLen, 60, cardNo.getBytes(), INJECTED_PIN_SLOT, PinAlgorithmModeEnum.ISO9564FMT1,
                    pinPadInputListener);
        } else {
            pinPad.inputOfflinePin(pinLen, 60, pinPadInputListener);
        }
    }

    public static String byte2Char(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append((char) bytes[i]);
        }
        return sb.toString();
    }

    public static int byteArrayToInt(byte[] bytes) {
        int value = 0;
        // 由高位到低位
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (bytes[i] & 0x000000FF) << shift;// 往高位游
        }
        return value;
    }

    public static int bytesToInt2(byte[] src, int offset) {
        int value;
        value = (int) (((src[offset] & 0xFF) << 24)
                | ((src[offset + 1] & 0xFF) << 16)
                | ((src[offset + 2] & 0xFF) << 8)
                | (src[offset + 3] & 0xFF));
        return value;
    }

    private void initEmvAidAndCapk() {
        Log.d("calling aid ", "init capk and aid");
        // AID

        emvHandler2.delAllAid();
        if (emvHandler2.getAidListNum() <= 0) {
            String[] newAids = new String[] {
                    // Verve
                    "9F0607A0000003710001DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0F0C89F6604620000805F2A0208409F1A020418",
                    "9F0607A0000003710002DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0F0C89F6604620000805F2A0208409F1A020418",

                    // VISA
                    "9F0608A000000003010101DF0101009F08020000DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                    "9F0608A000000003010102DF0101009F08020000DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                    "9F0607A0000000031010DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                    "9F0607A0000000032010DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                    "9F0607A0000000033010DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                    "9F0609A00000000305076010DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                    "9F0608A000000003101001DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                    "9F0608A000000003101002DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                    "9F0607A0000000032020DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                    "9F0607A0000000034010DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                    "9F0607A0000000035010DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                    "9F0607A0000000036010DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                    "9F0607A0000000036020DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                    "9F0607A0000000038002DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                    "9F0607A0000000038010DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                    "9F0607A0000000039010DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                    "9F0608A000000003101008DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                    "9F0608A000000003101009DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                    "9F0608A000000003101012DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                    // masterCard
                    // 9F0607A0000000041010DF0101009F08020002DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418
                    "9F0607A0000000041010DF010100DF1105FC50BC2000DF1205FC50BCF800DF130500000000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B8C89F6604620000805F2A0208409F1D086E7A000000000000",
                    "9F0607A0000000043060DF010100DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B8C89F6604620000805F2A0208409F1D086E7A000000000000",
                    // JCB
                    "9F0607A0000000651010DF010100DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000000000DF2006001000000000DF21060000000300009F3303E0B0C89F660462000080",
                    // UnionPay
                    "9F0608A000000333010101DF010100DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF1801009F7B06000000100000DF1906001000000000DF2006001000000000DF21060010000000009F3303E0B8C89F660422000080",
                    // "9F0608A000000333010102DF010100DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF1801019F7B06000000100000DF1906000000100000DF2006001000000000DF21061000001000009F3303E0F0C8",
                    "9F0608A000000333010102DF010100DF11050000000000DF12050000000000DF130500000000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF1801019F7B06000000100000DF1906001000000000DF2006001000000000DF21060010000000009F3303E028C89F660426000080",
                    "9F0608A000000333010103DF010100DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF1801019F7B06000000100000DF1906001000000000DF2006001000000000DF21060010000000009F3303E028C89F660426000080",
                    "9F0608A000000333010106DF010100DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF1801019F7B06000000100000DF1906001000000000DF2006001000000000DF21060010000000009F3303E028C89F660426000080",
                    // JCB
                    "9F0607A0000000651010DF0101009F09020200DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000000000DF2006000010000000DF21060000000000009F3303E040C89F6604640000805F2A0204189F1A020418",
                    // Rupay
                    "9f0607A0000005241010df1105fc5080a000df1205f85080f800df130504000000009f1b0400000000df150400000000df160199df170199df14039f3704df1801009f7b06000000100000df1906000000200000df2006000000500001df21060000005000015f2a0203569f1a0203569f090200029f3303e0d9c89f660436004000"
            };

            ArrayList<String> aidParaTlvList = new ArrayList<>(Arrays.asList(newAids));
            int ii = emvHandler2.setAidParaList(aidParaTlvList);

            Log.d("nexgo", "setAidParaList 2" + ii);

            if (ii == SdkResult.Fail) {
                Log.d("nexgo", "add new aids failed");
            }
        } else {
            Log.d("nexgo", "setAidParaList " + "already load aid");
        }

        // CAPK

        emvHandler2.delAllCapk();
        int capk_num = emvHandler2.getCapkListNum();
        Log.d("nexgo", "capk_num " + capk_num);
        if (capk_num <= 0) {
            String[] newCapks = new String[] {

                    // Verve Capk
                    "9F0607A00000037100019F220109DF05083230323431323331DF060101DF070101DF0281B0B036A8CAE0593A480976BFE84F8A67759E52B3D9F4A68CCC37FE720E594E5694CD1AE20E1B120D7A18FA5C70E044D3B12E932C9BBD9FDEA4BE11071EF8CA3AF48FF2B5DDB307FC752C5C73F5F274D4238A92B4FCE66FC93DA18E6C1CC1AA3CFAFCB071B67DAACE96D9314DB494982F5C967F698A05E1A8A69DA931B8E566270F04EAB575F5967104118E4F12ABFF9DEC92379CD955A10675282FE1B60CAD13F9BB80C272A40B6A344EA699FB9EFA6867DF040103DF0314319F3C608B67F1118C729B0E1516EAB07CB290C8",
                    "9F0607A00000037100019F220104DF05083230313731323331DF060101DF070101DF028190D13CD5E1B921E4E0F0D40E2DE14CCE73E3A34ED2DCFA826531D8195641091E37C8474D19B686E8243F089A69F7B18D2D34CB4824F228F7750F96D1EFBDFF881F259A8C04DE64915A3A3D7CB846135F4083C93CDE755BC808886F600542DFF085558D5EA7F45CB15EC835064AA856D602A0A44CD021F54CF8EC0CC680B54B3665ABE74A7C43D02897FF84BB4CB98BC91DDF040103DF03148B36A3E3D814CE6C6EBEAAF27674BB7BC67275B1",
                    "9F0607A00000037100019F220103DF05083230313731323331DF060101DF070101DF028190d06238b856cf2c8890a7f668ca17c19247498d193a7c11e7105dedeee6a873e8189e50493e9b17547c42ea4fa88bbef30bb6bc2409246ccc95f36622a7f4d92d46444f20b1b24bf63c5b28395d8ef18c23205c2119dfe5fba2fbfc311b2fe8a6a75b35a7dab72d421792a500cdfd8133b8a97d84a49c0bd22d52d06ea5e0ef3e471d47d8370c37aa48b564689d0035d9DF040103DF0314319F3C608B67F1118C729B0E1516EAB07CB290C8",
                    "9F0607A00000037100019F220106DF05083230323831323238DF060101DF070101DF0281F8D2DA0134B4DFC93A75EE8960C99896D50A91527B87BA7B16CDB77E5B6FDB750EB70B54026CADDA1D562C77A2C6DA541E94BC415D43E68489B16980F2E887C09E4CF90E2E639B179277BBA0E982CCD1F80521D1457209125B3ABCD309E1B92B5AEDA2EB1CBF933BEAD9CE7365E52B7D17FCB405AA28E5DE6AA3F08E764F745E70859ABCBA41E570A6E4367B3D6FECE723B73ABF3EB53DCDE3816E8A813460447021509D0DFDF2EEEE74CC35485FB55C26836EB3BF9C7DEBEE6C0B77B7BE059233801CF76B321FCA25FB1E63117AE1865E23161EC39D7B1FB84256C2BE72BF8EC771548DB9F00BEF77C509FADA15E2B53FF950D383F96211D3DF040103DF0314F5BAB84ECE5F8BD45511E5CA861B80C7E6C51F55",
                    "9F0607A00000037100029F220105DF05083230323431323331DF060101DF070101DF0281B0B036A8CAE0593A480976BFE84F8A67759E52B3D9F4A68CCC37FE720E594E5694CD1AE20E1B120D7A18FA5C70E044D3B12E932C9BBD9FDEA4BE11071EF8CA3AF48FF2B5DDB307FC752C5C73F5F274D4238A92B4FCE66FC93DA18E6C1CC1AA3CFAFCB071B67DAACE96D9314DB494982F5C967F698A05E1A8A69DA931B8E566270F04EAB575F5967104118E4F12ABFF9DEC92379CD955A10675282FE1B60CAD13F9BB80C272A40B6A344EA699FB9EFA6867DF040103DF0314676822D335AB0D2C3848418CB546DF7B6A6C32C0",
                    "9F0607A00000037100029F220104DF05083230313731323331DF060101DF070101DF028190D13CD5E1B921E4E0F0D40E2DE14CCE73E3A34ED2DCFA826531D8195641091E37C8474D19B686E8243F089A69F7B18D2D34CB4824F228F7750F96D1EFBDFF881F259A8C04DE64915A3A3D7CB846135F4083C93CDE755BC808886F600542DFF085558D5EA7F45CB15EC835064AA856D602A0A44CD021F54CF8EC0CC680B54B3665ABE74A7C43D02897FF84BB4CB98BC91DDF040103DF0314676822D335AB0D2C3848418CB546DF7B6A6C32C0",
                    "9F0607A00000037100029F220103DF05083230313731323331DF060101DF070101DF028190d06238b856cf2c8890a7f668ca17c19247498d193a7c11e7105dedeee6a873e8189e50493e9b17547c42ea4fa88bbef30bb6bc2409246ccc95f36622a7f4d92d46444f20b1b24bf63c5b28395d8ef18c23205c2119dfe5fba2fbfc311b2fe8a6a75b35a7dab72d421792a500cdfd8133b8a97d84a49c0bd22d52d06ea5e0ef3e471d47d8370c37aa48b564689d0035d9DF040103DF0314319F3C608B67F1118C729B0E1516EAB07CB290C8",
                    "9F0607A00000037100029F220106DF05083230323831323238DF060101DF070101DF0281F8D2DA0134B4DFC93A75EE8960C99896D50A91527B87BA7B16CDB77E5B6FDB750EB70B54026CADDA1D562C77A2C6DA541E94BC415D43E68489B16980F2E887C09E4CF90E2E639B179277BBA0E982CCD1F80521D1457209125B3ABCD309E1B92B5AEDA2EB1CBF933BEAD9CE7365E52B7D17FCB405AA28E5DE6AA3F08E764F745E70859ABCBA41E570A6E4367B3D6FECE723B73ABF3EB53DCDE3816E8A813460447021509D0DFDF2EEEE74CC35485FB55C26836EB3BF9C7DEBEE6C0B77B7BE059233801CF76B321FCA25FB1E63117AE1865E23161EC39D7B1FB84256C2BE72BF8EC771548DB9F00BEF77C509FADA15E2B53FF950D383F96211D3DF040103DF0314F5BAB84ECE5F8BD45511E5CA861B80C7E6C51F55",

                    "9F0605A0000000659F220109DF05083230303931323331DF060101DF070101DF028180B72A8FEF5B27F2B550398FDCC256F714BAD497FF56094B7408328CB626AA6F0E6A9DF8388EB9887BC930170BCC1213E90FC070D52C8DCD0FF9E10FAD36801FE93FC998A721705091F18BC7C98241CADC15A2B9DA7FB963142C0AB640D5D0135E77EBAE95AF1B4FEFADCF9C012366BDDA0455C1564A68810D7127676D493890BDDF040103DF03144410C6D51C2F83ADFD92528FA6E38A32DF048D0A",
                    "9F0605A0000000659F220110DF05083230313231323331DF060101DF070101DF02819099B63464EE0B4957E4FD23BF923D12B61469B8FFF8814346B2ED6A780F8988EA9CF0433BC1E655F05EFA66D0C98098F25B659D7A25B8478A36E489760D071F54CDF7416948ED733D816349DA2AADDA227EE45936203CBF628CD033AABA5E5A6E4AE37FBACB4611B4113ED427529C636F6C3304F8ABDD6D9AD660516AE87F7F2DDF1D2FA44C164727E56BBC9BA23C0285DF040103DF0314C75E5210CBE6E8F0594A0F1911B07418CADB5BAB",
                    "9F0605A0000000659F220112DF05083230313431323331DF060101DF070101DF0281B0ADF05CD4C5B490B087C3467B0F3043750438848461288BFEFD6198DD576DC3AD7A7CFA07DBA128C247A8EAB30DC3A30B02FCD7F1C8167965463626FEFF8AB1AA61A4B9AEF09EE12B009842A1ABA01ADB4A2B170668781EC92B60F605FD12B2B2A6F1FE734BE510F60DC5D189E401451B62B4E06851EC20EBFF4522AACC2E9CDC89BC5D8CDE5D633CFD77220FF6BBD4A9B441473CC3C6FEFC8D13E57C3DE97E1269FA19F655215B23563ED1D1860D8681DF040103DF0314874B379B7F607DC1CAF87A19E400B6A9E25163E8",
                    "9F0605A0000000659F220114DF05083230313631323331DF060101DF070101DF0281F8AEED55B9EE00E1ECEB045F61D2DA9A66AB637B43FB5CDBDB22A2FBB25BE061E937E38244EE5132F530144A3F268907D8FD648863F5A96FED7E42089E93457ADC0E1BC89C58A0DB72675FBC47FEE9FF33C16ADE6D341936B06B6A6F5EF6F66A4EDD981DF75DA8399C3053F430ECA342437C23AF423A211AC9F58EAF09B0F837DE9D86C7109DB1646561AA5AF0289AF5514AC64BC2D9D36A179BB8A7971E2BFA03A9E4B847FD3D63524D43A0E8003547B94A8A75E519DF3177D0A60BC0B4BAB1EA59A2CBB4D2D62354E926E9C7D3BE4181E81BA60F8285A896D17DA8C3242481B6C405769A39D547C74ED9FF95A70A796046B5EFF36682DC29DF040103DF0314C0D15F6CD957E491DB56DCDD1CA87A03EBE06B7B",
                    "9F0605A0000000659F220111DF05083230323531323331DF060101DF070101DF0281B0A2583AA40746E3A63C22478F576D1EFC5FB046135A6FC739E82B55035F71B09BEB566EDB9968DD649B94B6DEDC033899884E908C27BE1CD291E5436F762553297763DAA3B890D778C0F01E3344CECDFB3BA70D7E055B8C760D0179A403D6B55F2B3B083912B183ADB7927441BED3395A199EEFE0DEBD1F5FC3264033DA856F4A8B93916885BD42F9C1F456AAB8CFA83AC574833EB5E87BB9D4C006A4B5346BD9E17E139AB6552D9C58BC041195336485DF040103DF0314D9FD62C9DD4E6DE7741E9A17FB1FF2C5DB948BCB",
                    "9F0605A0000000659F220113DF05083230323531323331DF060101DF070101DF0281F8A3270868367E6E29349FC2743EE545AC53BD3029782488997650108524FD051E3B6EACA6A9A6C1441D28889A5F46413C8F62F3645AAEB30A1521EEF41FD4F3445BFA1AB29F9AC1A74D9A16B93293296CB09162B149BAC22F88AD8F322D684D6B49A12413FC1B6AC70EDEDB18EC1585519A89B50B3D03E14063C2CA58B7C2BA7FB22799A33BCDE6AFCBEB4A7D64911D08D18C47F9BD14A9FAD8805A15DE5A38945A97919B7AB88EFA11A88C0CD92C6EE7DC352AB0746ABF13585913C8A4E04464B77909C6BD94341A8976C4769EA6C0D30A60F4EE8FA19E767B170DF4FA80312DBA61DB645D5D1560873E2674E1F620083F30180BD96CA589DF040103DF031454CFAE617150DFA09D3F901C9123524523EBEDF3",
                    "9f0605a0000000039f220101df05083230313231323331df060101df070101df028180c696034213d7d8546984579d1d0f0ea519cff8deffc429354cf3a871a6f7183f1228da5c7470c055387100cb935a712c4e2864df5d64ba93fe7e63e71f25b1e5f5298575ebe1c63aa617706917911dc2a75ac28b251c7ef40f2365912490b939bca2124a30a28f54402c34aeca331ab67e1e79b285dd5771b5d9ff79ea630b75df040103df0314d34a6a776011c7e7ce3aec5f03ad2f8cfc5503cc",
                    "9f0605a0000000049f220103df05083230313231323331df060101df070101df028180c2490747fe17eb0584c88d47b1602704150adc88c5b998bd59ce043edebf0ffee3093ac7956ad3b6ad4554c6de19a178d6da295be15d5220645e3c8131666fa4be5b84fe131ea44b039307638b9e74a8c42564f892a64df1cb15712b736e3374f1bbb6819371602d8970e97b900793c7c2a89a4a1649a59be680574dd0b60145df040103df03145addf21d09278661141179cbeff272ea384b13bb",
                    "9f0605a0000003339f220109df05083230313231323331df060101df070101df0281b0eb374dfc5a96b71d2863875eda2eafb96b1b439d3ece0b1826a2672eeefa7990286776f8bd989a15141a75c384dfc14fef9243aab32707659be9e4797a247c2f0b6d99372f384af62fe23bc54bcdc57a9acd1d5585c303f201ef4e8b806afb809db1a3db1cd112ac884f164a67b99c7d6e5a8a6df1d3cae6d7ed3d5be725b2de4ade23fa679bf4eb15a93d8a6e29c7ffa1a70de2e54f593d908a3bf9ebbd760bbfdc8db8b54497e6c5be0e4a4dac29e5df040103df0314a075306eab0045baf72cdd33b3b678779de1f527",
                    "9f0605a0000003339f220104df05083230313431323331df060101df070101df0281f8bc853e6b5365e89e7ee9317c94b02d0abb0dbd91c05a224a2554aa29ed9fcb9d86eb9ccbb322a57811f86188aac7351c72bd9ef196c5a01acef7a4eb0d2ad63d9e6ac2e7836547cb1595c68bcbafd0f6728760f3a7ca7b97301b7e0220184efc4f653008d93ce098c0d93b45201096d1adff4cf1f9fc02af759da27cd6dfd6d789b099f16f378b6100334e63f3d35f3251a5ec78693731f5233519cdb380f5ab8c0f02728e91d469abd0eae0d93b1cc66ce127b29c7d77441a49d09fca5d6d9762fc74c31bb506c8bae3c79ad6c2578775b95956b5370d1d0519e37906b384736233251e8f09ad79dfbe2c6abfadac8e4d8624318c27daf1df040103df0314f527081cf371dd7e1fd4fa414a665036e0f5e6e5",
                    "9f0605a0000000659f220109df05083230313431323331df060101df070101df028180b72a8fef5b27f2b550398fdcc256f714bad497ff56094b7408328cb626aa6f0e6a9df8388eb9887bc930170bcc1213e90fc070d52c8dcd0ff9e10fad36801fe93fc998a721705091f18bc7c98241cadc15a2b9da7fb963142c0ab640d5d0135e77ebae95af1b4fefadcf9c012366bdda0455c1564a68810d7127676d493890bddf040103df03144410c6d51c2f83adfd92528fa6e38a32df048d0a",
                    "9f0605a0000000659f220110df05083230313231323331df060101df070101df02819099b63464ee0b4957e4fd23bf923d12b61469b8fff8814346b2ed6a780f8988ea9cf0433bc1e655f05efa66d0c98098f25b659d7a25b8478a36e489760d071f54cdf7416948ed733d816349da2aadda227ee45936203cbf628cd033aaba5e5a6e4ae37fbacb4611b4113ed427529c636f6c3304f8abdd6d9ad660516ae87f7f2ddf1d2fa44c164727e56bbc9ba23c0285df040103df0314c75e5210cbe6e8f0594a0f1911b07418cadb5bab",
                    "9f0605a0000003339f22010adf05083230313431323331df060101df070101df028180b2ab1b6e9ac55a75adfd5bbc34490e53c4c3381f34e60e7fac21cc2b26dd34462b64a6fae2495ed1dd383b8138bea100ff9b7a111817e7b9869a9742b19e5c9dac56f8b8827f11b05a08eccf9e8d5e85b0f7cfa644eff3e9b796688f38e006deb21e101c01028903a06023ac5aab8635f8e307a53ac742bdce6a283f585f48efdf040103df0314c88be6b2417c4f941c9371ea35a377158767e4e3",
                    "9f0605a0000000039f220108df05083230313431323331df060101df070101df0281b0d9fd6ed75d51d0e30664bd157023eaa1ffa871e4da65672b863d255e81e137a51de4f72bcc9e44ace12127f87e263d3af9dd9cf35ca4a7b01e907000ba85d24954c2fca3074825ddd4c0c8f186cb020f683e02f2dead3969133f06f7845166aceb57ca0fc2603445469811d293bfefbafab57631b3dd91e796bf850a25012f1ae38f05aa5c4d6d03b1dc2e568612785938bbc9b3cd3a910c1da55a5a9218ace0f7a21287752682f15832a678d6e1ed0bdf040103df031420d213126955de205adc2fd2822bd22de21cf9a8",
                    "9f0605a0000003339f220102df05083230313431323331df060101df070101df028190a3767abd1b6aa69d7f3fbf28c092de9ed1e658ba5f0909af7a1ccd907373b7210fdeb16287ba8e78e1529f443976fd27f991ec67d95e5f4e96b127cab2396a94d6e45cda44ca4c4867570d6b07542f8d4bf9ff97975db9891515e66f525d2b3cbeb6d662bfb6c3f338e93b02142bfc44173a3764c56aadd202075b26dc2f9f7d7ae74bd7d00fd05ee430032663d27a57df040103df031403bb335a8549a03b87ab089d006f60852e4b8060",
                    "9f0605a0000000659f220112df05083230313431323331df060101df070101df0281b0adf05cd4c5b490b087c3467b0f3043750438848461288bfefd6198dd576dc3ad7a7cfa07dba128c247a8eab30dc3a30b02fcd7f1c8167965463626feff8ab1aa61a4b9aef09ee12b009842a1aba01adb4a2b170668781ec92b60f605fd12b2b2a6f1fe734be510f60dc5d189e401451b62b4e06851ec20ebff4522aacc2e9cdc89bc5d8cde5d633cfd77220ff6bbd4a9b441473cc3c6fefc8d13e57c3de97e1269fa19f655215b23563ed1d1860d8681df040103df0314874b379b7f607dc1caf87a19e400b6a9e25163e8",
                    "9f0605a0000000039f220107df05083230313231323331df060101df070101df028190a89f25a56fa6da258c8ca8b40427d927b4a1eb4d7ea326bbb12f97ded70ae5e4480fc9c5e8a972177110a1cc318d06d2f8f5c4844ac5fa79a4dc470bb11ed635699c17081b90f1b984f12e92c1c529276d8af8ec7f28492097d8cd5becea16fe4088f6cfab4a1b42328a1b996f9278b0b7e3311ca5ef856c2f888474b83612a82e4e00d0cd4069a6783140433d50725fdf040103df0314b4bc56cc4e88324932cbc643d6898f6fe593b172",
                    "9f0605a0000000049f220163df05083230313231323331df060101df070101df028190cf71f040528c9af2bf4341c639b7f31be1abff269633542cf22c03ab51570402c9cafc14437ae42f4e7cad00c9811b536dff3792facb86a0c7fae5fa50ae6c42546c534ea3a11fbd2267f1cf9ac68874dc221ecb3f6334f9c0bb832c075c2961ca9bbb683bec2477d12344e1b7d6dbe07b286fcf41a0f7f1f6f248a8c86398b7fa1c115111051dd01df3ed08985705fddf040103df03146e5ff80cd0a1cc2e3249b9c198d43427ce874013",
                    "9f0605a0000000049f220104df05083230313231323331df060101df070101df028190a6da428387a502d7ddfb7a74d3f412be762627197b25435b7a81716a700157ddd06f7cc99d6ca28c2470527e2c03616b9c59217357c2674f583b3ba5c7dcf2838692d023e3562420b4615c439ca97c44dc9a249cfce7b3bfb22f68228c3af13329aa4a613cf8dd853502373d62e49ab256d2bc17120e54aedced6d96a4287acc5c04677d4a5a320db8bee2f775e5fec5df040103df0314381a035da58b482ee2af75f4c3f2ca469ba4aa6c",
                    "9f0605a0000000039f220109df05083230313631323331df060101df070101df0281f89d912248de0a4e39c1a7dde3f6d2588992c1a4095afbd1824d1ba74847f2bc4926d2efd904b4b54954cd189a54c5d1179654f8f9b0d2ab5f0357eb642feda95d3912c6576945fab897e7062caa44a4aa06b8fe6e3dba18af6ae3738e30429ee9be03427c9d64f695fa8cab4bfe376853ea34ad1d76bfcad15908c077ffe6dc5521ecef5d278a96e26f57359ffaeda19434b937f1ad999dc5c41eb11935b44c18100e857f431a4a5a6bb65114f174c2d7b59fdf237d6bb1dd0916e644d709ded56481477c75d95cdd68254615f7740ec07f330ac5d67bcd75bf23d28a140826c026dbde971a37cd3ef9b8df644ac385010501efc6509d7a41df040103df03141ff80a40173f52d7d27e0f26a146a1c8ccb29046",
                    "9f0605a0000000039f220163df05083230313231323331df060101df070101df028190cf71f040528c9af2bf4341c639b7f31be1abff269633542cf22c03ab51570402c9cafc14437ae42f4e7cad00c9811b536dff3792facb86a0c7fae5fa50ae6c42546c534ea3a11fbd2267f1cf9ac68874dc221ecb3f6334f9c0bb832c075c2961ca9bbb683bec2477d12344e1b7d6dbe07b286fcf41a0f7f1f6f248a8c86398b7fa1c115111051dd01df3ed08985705fddf040103df0314b2f6af1ddc393be17525d0ea7bf568bed5b71167",
                    "9f0605a0000000049f220106df05083230313631323331df060101df070101df0281f8cb26fc830b43785b2bce37c81ed334622f9622f4c89aae641046b2353433883f307fb7c974162da72f7a4ec75d9d657336865b8d3023d3d645667625c9a07a6b7a137cf0c64198ae38fc238006fb2603f41f4f3bb9da1347270f2f5d8c606e420958c5f7d50a71de30142f70de468889b5e3a08695b938a50fc980393a9cbce44ad2d64f630bb33ad3f5f5fd495d31f37818c1d94071342e07f1bec2194f6035ba5ded3936500eb82dfda6e8afb655b1ef3d0d7ebf86b66dd9f29f6b1d324fe8b26ce38ab2013dd13f611e7a594d675c4432350ea244cc34f3873cba06592987a1d7e852adc22ef5a2ee28132031e48f74037e3b34ab747fdf040103df0314f910a1504d5ffb793d94f3b500765e1abcad72d9",
                    "9f0605a0000000049f220105df05083230313431323331df060101df070101df0281b0b8048abc30c90d976336543e3fd7091c8fe4800df820ed55e7e94813ed00555b573feca3d84af6131a651d66cff4284fb13b635edd0ee40176d8bf04b7fd1c7bacf9ac7327dfaa8aa72d10db3b8e70b2ddd811cb4196525ea386acc33c0d9d4575916469c4e4f53e8e1c912cc618cb22dde7c3568e90022e6bba770202e4522a2dd623d180e215bd1d1507fe3dc90ca310d27b3efccd8f83de3052cad1e48938c68d095aac91b5f37e28bb49ec7ed597df040103df0314ebfa0d5d06d8ce702da3eae890701d45e274c845",
                    "9f0605a0000003339f220101df05083230313431323331df060101df070101df028180bbe9066d2517511d239c7bfa77884144ae20c7372f515147e8ce6537c54c0a6a4d45f8ca4d290870cda59f1344ef71d17d3f35d92f3f06778d0d511ec2a7dc4ffeadf4fb1253ce37a7b2b5a3741227bef72524da7a2b7b1cb426bee27bc513b0cb11ab99bc1bc61df5ac6cc4d831d0848788cd74f6d543ad37c5a2b4c5d5a93bdf040103df0314e881e390675d44c2dd81234dce29c3f5ab2297a0",
                    "9f0605a0000003339f220108df05083230323031323331df060101df070101df028190b61645edfd5498fb246444037a0fa18c0f101ebd8efa54573ce6e6a7fbf63ed21d66340852b0211cf5eef6a1cd989f66af21a8eb19dbd8dbc3706d135363a0d683d046304f5a836bc1bc632821afe7a2f75da3c50ac74c545a754562204137169663cfcc0b06e67e2109eba41bc67ff20cc8ac80d7b6ee1a95465b3b2657533ea56d92d539e5064360ea4850fed2d1bfdf040103df0314ee23b616c95c02652ad18860e48787c079e8e85a",
                    "9f0605a0000003339f220103df05083230313431323331df060101df070101df0281b0b0627dee87864f9c18c13b9a1f025448bf13c58380c91f4ceba9f9bcb214ff8414e9b59d6aba10f941c7331768f47b2127907d857fa39aaf8ce02045dd01619d689ee731c551159be7eb2d51a372ff56b556e5cb2fde36e23073a44ca215d6c26ca68847b388e39520e0026e62294b557d6470440ca0aefc9438c923aec9b2098d6d3a1af5e8b1de36f4b53040109d89b77cafaf70c26c601abdf59eec0fdc8a99089140cd2e817e335175b03b7aa33ddf040103df031487f0cd7c0e86f38f89a66f8c47071a8b88586f26",
                    "9f0605a0000003339f22010bdf05083230313631323331df060101df070101df0281f8cf9fdf46b356378e9af311b0f981b21a1f22f250fb11f55c958709e3c7241918293483289eae688a094c02c344e2999f315a72841f489e24b1ba0056cfab3b479d0e826452375dcdbb67e97ec2aa66f4601d774feaef775accc621bfeb65fb0053fc5f392aa5e1d4c41a4de9ffdfdf1327c4bb874f1f63a599ee3902fe95e729fd78d4234dc7e6cf1ababaa3f6db29b7f05d1d901d2e76a606a8cbffffecbd918fa2d278bdb43b0434f5d45134be1c2781d157d501ff43e5f1c470967cd57ce53b64d82974c8275937c5d8502a1252a8a5d6088a259b694f98648d9af2cb0efd9d943c69f896d49fa39702162acb5af29b90bade005bc157df040103df0314bd331f9996a490b33c13441066a09ad3feb5f66c",
                    "9f0605a0000000659f220114df05083230313631323331df060101df070101df0281f8aeed55b9ee00e1eceb045f61d2da9a66ab637b43fb5cdbdb22a2fbb25be061e937e38244ee5132f530144a3f268907d8fd648863f5a96fed7e42089e93457adc0e1bc89c58a0db72675fbc47fee9ff33c16ade6d341936b06b6a6f5ef6f66a4edd981df75da8399c3053f430eca342437c23af423a211ac9f58eaf09b0f837de9d86c7109db1646561aa5af0289af5514ac64bc2d9d36a179bb8a7971e2bfa03a9e4b847fd3d63524d43a0e8003547b94a8a75e519df3177d0a60bc0b4bab1ea59a2cbb4d2d62354e926e9c7d3be4181e81ba60f8285a896d17da8c3242481b6c405769a39d547c74ed9ff95a70a796046b5eff36682dc29df040103df0314c0d15f6cd957e491db56dcdd1ca87a03ebe06b7b"
            };

            ArrayList<String> capKParaTlvList = new ArrayList<>(Arrays.asList(newCapks));
            int jj = emvHandler2.setCAPKList(capKParaTlvList);

            Log.d("nexgo", "setCapKList 2" + jj);

            if (jj == SdkResult.Fail) {
                Log.d("nexgo", "add new capks failed");
            }
        } else {
            Log.d("nexgo", "setCAPKList " + "already load capk");
        }

    }

    public void checkCard(@NonNull MethodChannel.Result result) {
        if (deviceEngine == null) {
            deviceEngine = APIProxy.getDeviceEngine(mContext);
        }

        CardReader cardReader = deviceEngine.getCardReader();
        boolean cardExist = cardReader.isCardExist(CardSlotTypeEnum.ICC1);
        runOnUiThread(() -> result.success(cardExist));
    }

    private void injectKSN(PinPad pinPad) {
        byte[] iPekByte = hexToByteArr("3F2216D8297BCE9C");
        int i = pinPad.dukptKeyInject(0, DukptKeyTypeEnum.IPEK, iPekByte, iPekByte.length, hexToByteArr(getKSN()));
        Log.d("nexgo", "key inject " + i);
    }

    private byte[] hexToByteArr(String hexString) {
        if (hexString == null)
            return null;
        byte[] byteArray = new byte[hexString.length() / 2];
        for (int i = 0; i < byteArray.length; i++) {
            int index = i * 2;
            int j = Integer.parseInt(hexString.substring(index, index + 2), 16);
            byteArray[i] = (byte) j;
        }

        return byteArray;
    }

    private String getKSN() {
        return "0000000002DDDDE00001";
    }

    private String formatAmount(int amount) {
        DecimalFormat decimalFormat = new DecimalFormat("#,###");
        return "₦ " + decimalFormat.format(amount) + ".00";
    }

    private int amountForDisplay() {
        return (Integer.parseInt(this.amount) + this.charge) / 100;
    }

}
