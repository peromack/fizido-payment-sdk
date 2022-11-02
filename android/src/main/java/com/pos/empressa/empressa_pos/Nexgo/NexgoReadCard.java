package com.pos.empressa.empressa_pos.Nexgo;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.nexgo.common.ByteUtils;
import com.nexgo.common.LogUtils;
import com.nexgo.oaf.apiv3.APIProxy;
import com.nexgo.oaf.apiv3.DeviceEngine;
import com.nexgo.oaf.apiv3.SdkResult;
import com.nexgo.oaf.apiv3.device.pinpad.AlgorithmModeEnum;
import com.nexgo.oaf.apiv3.device.pinpad.DesAlgorithmModeEnum;
import com.nexgo.oaf.apiv3.device.pinpad.DukptKeyModeEnum;
import com.nexgo.oaf.apiv3.device.pinpad.DukptKeyTypeEnum;
import com.nexgo.oaf.apiv3.device.pinpad.MacAlgorithmModeEnum;
import com.nexgo.oaf.apiv3.device.pinpad.OnPinPadInputListener;
import com.nexgo.oaf.apiv3.device.pinpad.PinAlgorithmModeEnum;
import com.nexgo.oaf.apiv3.device.pinpad.PinKeyboardModeEnum;
import com.nexgo.oaf.apiv3.device.pinpad.PinPad;
import com.nexgo.oaf.apiv3.device.pinpad.PinPadKeyCode;
import com.nexgo.oaf.apiv3.device.pinpad.PinpadLayoutEntity;
import com.nexgo.oaf.apiv3.device.pinpad.WorkKeyTypeEnum;
import com.nexgo.oaf.apiv3.device.reader.CardInfoEntity;
import com.nexgo.oaf.apiv3.device.reader.CardReader;
import com.nexgo.oaf.apiv3.device.reader.CardSlotTypeEnum;
import com.nexgo.oaf.apiv3.device.reader.OnCardInfoListener;
import com.nexgo.oaf.apiv3.emv.AidEntity;
import com.nexgo.oaf.apiv3.emv.AmexTransDataEntity;
import com.nexgo.oaf.apiv3.emv.CandidateAppInfoEntity;
import com.nexgo.oaf.apiv3.emv.CapkEntity;
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
import com.pos.empressa.empressa_pos.Nexgo.utils.EmvUtils;
import com.pos.empressa.empressa_pos.ksnUtil.KSNUtilities;
import com.socsi.utils.HexUtil;
import com.socsi.utils.TlvUtil;
import com.xinguodu.ddiinterface.KeyCode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import io.flutter.plugin.common.MethodChannel;

public class NexgoReadCard extends Application {
    private DeviceEngine deviceEngine;
    private EmvHandler2 emvHandler2;
    private String cardNo;
    private Context mContext;
    private EmvUtils emvUtils;

    private PinPad pinPad;

    String amount ;

    String cardPin = "";

    private String pinBlockArray;
    private String pinKsn;

    MethodChannel.Result results;

    private CardSlotTypeEnum mExistSlot;
    private boolean flag = true;

    private boolean isExpressPaySeePhoneTapCardAgain = false;


    public NexgoReadCard (Context mContext) {
        this.mContext = mContext;
    }

    public void searchCard(@NonNull MethodChannel.Result result, int transactionAmount) {
        try{

            deviceEngine = APIProxy.getDeviceEngine(mContext);

            pinPad = deviceEngine.getPinPad();

            emvHandler2 = deviceEngine.getEmvHandler2("app2");

            //enable below lines to capture the EMV logs
            emvHandler2.emvDebugLog(true);
            LogUtils.setDebugEnable(true);

            emvUtils = new EmvUtils(mContext);

            initEmvAidAndCapk();

            amount = String.valueOf(transactionAmount);

            results = result;

            startEmvTest(result);

        }catch (Exception e) {
            e.printStackTrace();
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
                    EmvTransConfigurationEntity transData = new EmvTransConfigurationEntity();
                    transData.setTransAmount(amount);
//            transData.setCashbackAmount("000000000100"); //if support cashback amount
                    transData.setEmvTransType((byte) 0x00); //0x00-sale, 0x20-refund,0x09-sale with cashback
                    transData.setCountryCode("566");    //CountryCode
                    transData.setCurrencyCode("566");    //CurrencyCode, 566 indicate Nigerian Naira
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


                    if(isExpressPaySeePhoneTapCardAgain){
                        AmexTransDataEntity amexTransDataEntity = new AmexTransDataEntity();
                        amexTransDataEntity.setExpressPaySeePhoneTapCardAgain(true);
                    }

                    //for UPI
                    UnionPayTransDataEntity unionPayTransDataEntity = new UnionPayTransDataEntity();
                    unionPayTransDataEntity.setQpbocForGlobal(true);
                    unionPayTransDataEntity.setSupportCDCVM(true);
                    //if support QPS, please enable below lines
                    //unionPayTransDataEntity.setSupportContactlessQps(true);
                    //unionPayTransDataEntity.setContactlessQpsLimit("000000030000");
                    transData.setUnionPayTransDataEntity(unionPayTransDataEntity);


                    //if you want set contactless aid for first select, you can enable below lines. it is only used for contactless
                    //for example, the card have paypass and pure application(paypass priority is highest), but the local bank required use pure application,
                    // in this situation , you can use below method.
//            emvHandler2.contactlessSetAidFirstSelect((byte) 0x07, ByteUtils.hexString2ByteArray("a0000000041010"));
//            emvHandler2.contactlessSetAidFirstSelect((byte) 0x07, ByteUtils.hexString2ByteArray("a0000001524010"));

                    Log.d("nexgo", "start emv " );
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
                //cardReader.stopSearch(); //before next search card, please stopSearch first

                Log.d("Search Card", "please tap one card");
            }
        });
//        Toast.makeText(mContext, "please insert or tap card", Toast.LENGTH_SHORT).show();

    }

    private void readcard(@NonNull MethodChannel.Result result, EmvTransConfigurationEntity transData) {
        Log.d("Read Card", "Calling Read Card");
        emvHandler2.emvProcess(transData, new OnEmvProcessListener2() {
            @Override
            public void onSelApp(final List<String> appNameList, List<CandidateAppInfoEntity> appInfoList, boolean isFirstSelect) {
                Log.d("nexgo", "onAfterFinalSelectedApp->");
            }

            @Override
            public void onTransInitBeforeGPO() {
                Log.d("nexgo",  "onAfterFinalSelectedApp" );
                byte[] aid = emvHandler2.getTlv(new byte[]{0x4F}, EmvDataSourceEnum.FROM_KERNEL);

                if (mExistSlot == CardSlotTypeEnum.RF) {
                    if (aid != null) {
                        if(ByteUtils.byteArray2HexString(aid).toUpperCase().contains("A000000004")){
                            //Paypass
                            configPaypassParameter(aid);
                        }else if(ByteUtils.byteArray2HexString(aid).toUpperCase().contains("A000000003")){
                            //Paywave
//                    configPaywaveParameters();
                        }else if(ByteUtils.byteArray2HexString(aid).toUpperCase().contains("A000000025")){
                            //ExpressPay
//                    configExpressPayParameter();
                        }else if(ByteUtils.byteArray2HexString(aid).toUpperCase().contains("A000000541")){
                            //configPureContactlessParameter();
                        }else if(ByteUtils.byteArray2HexString(aid).toUpperCase().contains("A000000065")){
                            //configJcbContactlessParameter();
                        }
                    }
                }else{
                    //contact terminal capability ; if different card brand(depend on aid) have different terminal capability
//            if(ByteUtils.byteArray2HexString(aid).toUpperCase().contains("A000000004")){
//                emvHandler2.setTlv(new byte[]{(byte)0x9F,(byte)0x33}, new byte[]{(byte)0xE0,(byte)0xF8,(byte)0xC8});
//                emvHandler2.setTlv(new byte[]{(byte)0x9F,(byte)0x1D}, ByteUtils.hexString2ByteArray("6C00800000000000"));//terminal risk

//            }
                }


                emvHandler2.onSetTransInitBeforeGPOResponse(true);
            }

            @Override
            public void onContactlessTapCardAgain() {
                Log.d("nexgo", "onReadCardAgain");

                //this method only used for EMV contactless card if the host response the script. Such as paywave , AMEX...

                //for paywave, onOnlineProc-->onSetOnlineProcResponse->onContactlessTapCardAgain--> search contactless card ->onReadCardAgainResponse->onFinish

//        emvHandler.onSetReadCardAgainResponse(true);
            }

            @Override
            public void onConfirmCardNo(final CardInfoEntity cardInfo) {
                Log.d("nexgo",  "onConfirmCardNo" + new Gson().toJson(cardInfo) );
                Log.d("nexgo",  "onConfirmCardNo" + cardInfo.getTk2() );
                Log.d("nexgo",  "onConfirmCardNo" + cardInfo.getCardNo() );
                if(mExistSlot == CardSlotTypeEnum.RF ){
                    emvHandler2.onSetConfirmCardNoResponse(true);
                    return ;
                }

                cardNo = cardInfo.getCardNo();
                emvHandler2.onSetConfirmCardNoResponse(true);
            }

            @Override
            public void onCardHolderInputPin(final boolean isOnlinePin, int leftTimes) {
                Log.d("nexgo",  "onCardHolderInputPin isOnlinePin = " + isOnlinePin);
                Log.d("nexgo",  "onCardHolderInputPin leftTimes = " + leftTimes);

                showInputPin(isOnlinePin);

            }


            @Override
            public void onRemoveCard() {
                Log.d("nexgo",  "onRemoveCard" );

                emvHandler2.onSetRemoveCardResponse();
                emvHandler2.emvProcessCancel();
            }


            @Override
            public void onPrompt(PromptEnum promptEnum) {
                Log.d("nexgo",  "onPrompt->" + promptEnum);
                emvHandler2.onSetPromptResponse(true);
            }


            @Override
            public void onOnlineProc() {
                Log.d("nexgo", "onOnlineProc");

                Log.d("nexgo", "getEmvContactlessMode:" + emvHandler2.getEmvContactlessMode());
                Log.d("nexgo", "getcardinfo:" + new Gson().toJson(emvHandler2.getEmvCardDataInfo()));
                Log.d("nexgo", "getEmvCvmResult:" + emvHandler2.getEmvCvmResult());
                Log.d("nexgo", "getSignNeed--" + emvHandler2.getSignNeed());

                byte[] tlv_5A = emvHandler2.getTlv(new byte[]{(byte) 0x5A}, EmvDataSourceEnum.FROM_KERNEL);
                Log.d("nexgo", "tlv_5A--" + ByteUtils.byteArray2HexString(tlv_5A));

                byte[] tlv_95 = emvHandler2.getTlv(new byte[]{(byte) 0x95}, EmvDataSourceEnum.FROM_KERNEL);
                Log.d("nexgo", "tlv_95--" + ByteUtils.byteArray2HexString(tlv_95));


                byte[] tlv_84 = emvHandler2.getTlv(new byte[]{(byte) 0x84}, EmvDataSourceEnum.FROM_KERNEL);
                Log.d("nexgo", "tlv_84--" + ByteUtils.byteArray2HexString(tlv_84));

                byte[] tlv_50 = emvHandler2.getTlv(new byte[]{(byte) 0x50}, EmvDataSourceEnum.FROM_KERNEL);
                Log.d("nexgo", "tlv_50--" + ByteUtils.byteArray2HexString(tlv_50));

                EmvOnlineResultEntity emvOnlineResult = new EmvOnlineResultEntity();
                emvOnlineResult.setAuthCode("123450");
                emvOnlineResult.setRejCode("00");
                //fill with the host response 55 field EMV data to do second auth, the format should be TLV format.
                // for example: 910870741219600860008a023030  91 = tag, 08 = len, 7074121960086000 = value;
                // 8a = tag, 02 = len, 3030 = value
                emvOnlineResult.setRecvField55(null);
                emvHandler2.onSetOnlineProcResponse(SdkResult.Success, emvOnlineResult);

            }

            @Override
            public void onFinish(final int retCode, EmvProcessResultEntity entity) {
                Log.d("nexgo", "onFinish" + "retCode :" + retCode );

                boolean flag = false;
                byte[] aid = emvHandler2.getTlv(new byte[]{0x4F}, EmvDataSourceEnum.FROM_KERNEL);
                if(aid != null){
                    if(mExistSlot == CardSlotTypeEnum.RF){
                        if(ByteUtils.byteArray2HexString(aid).toUpperCase().contains("A000000025")){
                            if(retCode == SdkResult.Emv_Plz_See_Phone){
                                isExpressPaySeePhoneTapCardAgain = true;
                                flag = true;
                            }
                        }
                    }
                }
                if(!flag){
                    isExpressPaySeePhoneTapCardAgain = false;
                }

                String tlvData = emvHandler2.getTlvByTags(EmvUtils.tags);

                Log.d("nexgo", "tlvData:" + tlvData);

                //Building Card data Map
                HashMap<String, String> cardDataMap = (HashMap<String, String>) TlvUtil.tlvToMap(tlvData);

                KSNUtilities ksnUtilitites = new KSNUtilities();
                String workingKey2 = ksnUtilitites.getWorkingKey("3F2216D8297BCE9C",pinKsn);
                Log.d(NexgoReadCard.class.getName(),"pinKSn 2" + ksnUtilitites.getLatestKsn());

                cardDataMap.put("pan", cardNo);
                cardDataMap.put("CardPin",pinBlockArray);
                cardDataMap.put("ksn",ksnUtilitites.getLatestKsn());

                pinPad.dukptKsnIncrease(0);

                Log.d(NexgoReadCard.class.getName(), ">>>onCompleted :" + cardDataMap + "\n" + ".............." + "\n" +
                        "pinKSn " + pinKsn + "\n" + "pinBlockArray " + pinBlockArray );

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

                //get CVM result
                Log.d("nexgo", "getEmvCvmResult:" + emvHandler2.getEmvCvmResult());

                Log.d("nexgo", "emvHandler2.getSignNeed()--" + emvHandler2.getSignNeed());

                //get card number, track 2 data...etc
                Log.d("nexgo", "getcardinfo:" + new Gson().toJson(emvHandler2.getEmvCardDataInfo()));


                byte[] tlv_5A = emvHandler2.getTlv(new byte[]{(byte) 0x5A}, EmvDataSourceEnum.FROM_KERNEL);
                Log.d("nexgo", "tlv_5A--" + ByteUtils.byteArray2HexString(tlv_5A));

                byte[] tlv_95 = emvHandler2.getTlv(new byte[]{(byte) 0x95}, EmvDataSourceEnum.FROM_KERNEL);
                Log.d("nexgo", "tlv_95--" + ByteUtils.byteArray2HexString(tlv_95));


                byte[] tlv_84 = emvHandler2.getTlv(new byte[]{(byte) 0x84}, EmvDataSourceEnum.FROM_KERNEL);
                Log.d("nexgo", "tlv_84--" + ByteUtils.byteArray2HexString(tlv_84));

                byte[] tlv_50 = emvHandler2.getTlv(new byte[]{(byte) 0x50}, EmvDataSourceEnum.FROM_KERNEL);
                Log.d("nexgo", "tlv_50--" + ByteUtils.byteArray2HexString(tlv_50));

//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                Toast.makeText(EmvActivity2.this, retCode + "", Toast.LENGTH_SHORT).show();
//            }
//        });

                switch (retCode){
                    case SdkResult.Emv_Success_Arpc_Fail:
                    case SdkResult.Success:
                    case SdkResult.Emv_Script_Fail:
                        //online approve
                        break;

                    case SdkResult.Emv_Qpboc_Offline:// EMV Contactless: Offline Approval
                    case SdkResult.Emv_Offline_Accept://EMV Contact: Offline Approval
                        //offline approve
                        break;

                    //this retcode is Abolished
                    case SdkResult.Emv_Qpboc_Online://EMV Contactless: Online Process for union pay
                        //union pay online contactless--application should go online
                        break;

                    case SdkResult.Emv_Candidatelist_Empty:// Application have no aid list
                    case SdkResult.Emv_FallBack://  FallBack ,chip card reset failed
                        //fallback process
                        break;

                    case SdkResult.Emv_Arpc_Fail: //
                    case SdkResult.Emv_Declined:
                        //online decline ,if it is in second gac, application should decide if it is need reversal the transaction
                        break;

                    case SdkResult.Emv_Cancel:// Transaction Cancel
                        //user cancel
                        break;

                    case SdkResult.Emv_Offline_Declined: //
                        //offline decline
                        break;

                    case SdkResult.Emv_Card_Block: //Card Block
                        //card is blocked
                        break;

                    case SdkResult.Emv_App_Block: // Application Block
                        //card application block
                        break;

                    case SdkResult.Emv_App_Ineffect:
                        //card not active
                        break;

                    case SdkResult.Emv_App_Expired:
                        //card Expired
                        break;

                    case SdkResult.Emv_Other_Interface:
                        //try other entry mode, like contact or mag-stripe
                        break;

                    case SdkResult.Emv_Plz_See_Phone:
                        //see phone flow
                        //prompt a dialog to user to check phone-->search contactless card(another card) -->start new emvProcess again
                        break;

                    case SdkResult.Emv_Terminate:
                        //transaction terminate
                        break;

                    default:
                        //other error
                        break;
                }
                emvHandler2.emvProcessCancel();
            }
        });

    }

    private void setExpressPayDrl(){
        DynamicReaderLimitEntity defaultDynamicReaderLimitEntity = new DynamicReaderLimitEntity();
        defaultDynamicReaderLimitEntity.setAppProgID(new byte[]{(byte) 0xFF});
        defaultDynamicReaderLimitEntity.setReaderCVMReqLimit(ByteUtils.hexString2ByteArray("000000001000"));
        defaultDynamicReaderLimitEntity.setReaderContactlessTransLimit(ByteUtils.hexString2ByteArray("000000001500"));
        defaultDynamicReaderLimitEntity.setReaderContactlessFloorLimit(ByteUtils.hexString2ByteArray("000000001200"));

        List<DynamicReaderLimitEntity> dynamicReaderLimitEntities = new ArrayList<>();
        DynamicReaderLimitEntity dynamicReaderLimitEntity = new DynamicReaderLimitEntity();
        dynamicReaderLimitEntity.setAppProgID(new byte[]{(byte) 0x06});
        dynamicReaderLimitEntity.setReaderCVMReqLimit(ByteUtils.hexString2ByteArray("000000000200"));
        dynamicReaderLimitEntity.setReaderContactlessTransLimit(ByteUtils.hexString2ByteArray("000000000700"));
        dynamicReaderLimitEntity.setReaderContactlessFloorLimit(ByteUtils.hexString2ByteArray("000000000400"));
        dynamicReaderLimitEntities.add(dynamicReaderLimitEntity);

        dynamicReaderLimitEntity = new DynamicReaderLimitEntity();
        dynamicReaderLimitEntity.setAppProgID(new byte[]{(byte) 0x0B});
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
        entity.setAppProgID(new byte[]{0x31, 0x02, 0x68, 0x26, 0x20});//get from 9f5a
        entity.setAuthOfZeroCheck(true);
        entity.setStatusCheck(false);
        entity.setReaderCVMReqLimitCheck(true);
        entity.setReaderContactlessFloorLimitCheck(true);
        entity.setReaderContactlessTransLimitCheck(false);
        entity.setReaderCVMReqLimit(new byte[]{0x00, 0x00, 0x00, 0x00, 0x50, 0x01});
        entity.setReaderContactlessFloorLimit(new byte[]{0x00, 0x00, 0x00, 0x00, 0x50, 0x00});
        entity.setReaderContactlessTransLimit(new byte[]{0x00, 0x00, 0x00, 0x00, 0x60, 0x01});
        dynamicReaderLimitEntity.add(entity);

        DynamicReaderLimitEntity entity1 = new DynamicReaderLimitEntity();
        entity1.setDrlSupport(true);
        entity1.setAppProgID(new byte[]{0x31, 0x02, 0x68, 0x26, 0x12, 0x00,0x00,0x03});//get from 9f5a
        entity1.setStatusCheck(false);
        entity1.setAuthOfZeroCheck(true);
        entity1.setReaderCVMReqLimitCheck(true);
        entity1.setReaderContactlessFloorLimitCheck(true);
        entity1.setReaderContactlessTransLimitCheck(false);
        entity1.setReaderCVMReqLimit(new byte[]{0x00, 0x00, 0x00, 0x00, 0x50, 0x01});
        entity1.setReaderContactlessFloorLimit(new byte[]{0x00, 0x00, 0x00, 0x00, 0x50, 0x00});
        entity1.setReaderContactlessTransLimit(new byte[]{0x00, 0x00, 0x00, 0x00, 0x60, 0x01});
        dynamicReaderLimitEntity.add(entity1);

        DynamicReaderLimitEntity entity2 = new DynamicReaderLimitEntity();
        entity2.setDrlSupport(true);
        entity2.setAppProgID(new byte[]{0x31, 0x02, 0x68, 0x26, 0x12});//get from 9f5a
        entity2.setAuthOfZeroCheck(true);
        entity2.setStatusCheck(false);
        entity2.setReaderCVMReqLimitCheck(true);
        entity2.setReaderContactlessFloorLimitCheck(true);
        entity2.setReaderContactlessTransLimitCheck(false);
        entity2.setReaderCVMReqLimit(new byte[]{0x00, 0x00, 0x00, 0x00, 0x50, 0x01});
        entity2.setReaderContactlessFloorLimit(new byte[]{0x00, 0x00, 0x00, 0x00, 0x50, 0x00});
        entity2.setReaderContactlessTransLimit(new byte[]{0x00, 0x00, 0x00, 0x00, 0x60, 0x01});
        dynamicReaderLimitEntity.add(entity2);

        DynamicReaderLimitEntity entity3 = new DynamicReaderLimitEntity();
        entity3.setDrlSupport(true);
        entity3.setAppProgID(new byte[]{0x31, 0x02, 0x68, 0x26,0x00});//get from 9f5a
        entity3.setAuthOfZeroCheck(true);
        entity3.setStatusCheck(false);
        entity3.setReaderCVMReqLimitCheck(true);
        entity3.setReaderContactlessFloorLimitCheck(true);
        entity3.setReaderContactlessTransLimitCheck(false);
        entity3.setReaderCVMReqLimit(new byte[]{0x00, 0x00, 0x00, 0x00, 0x50, 0x01});
        entity3.setReaderContactlessFloorLimit(new byte[]{0x00, 0x00, 0x00, 0x00, 0x50, 0x00});
        entity3.setReaderContactlessTransLimit(new byte[]{0x00, 0x00, 0x00, 0x00, 0x60, 0x01});
        dynamicReaderLimitEntity.add(entity3);

        emvHandler2.setDynamicReaderLimitListForPaywave(dynamicReaderLimitEntity);
    }

    private void configPaywaveParameters(){
    }


    private void configPaypassParameter(byte[] aid){
        //kernel configuration, enable RRP and cdcvm
        emvHandler2.setTlv(new byte[]{(byte) 0xDF, (byte) 0x81, (byte) 0x1B}, new byte[]{(byte) 0x30});

        //EMV MODE :amount >contactless cvm limit, set 60 = online pin and signature
        emvHandler2.setTlv(new byte[]{(byte) 0xDF, (byte) 0x81, (byte) 0x18}, new byte[]{(byte) 0x60});
        //EMV mode :amount < contactless cvm limit, set 08 = no cvm
        emvHandler2.setTlv(new byte[]{(byte) 0xDF, (byte) 0x81, (byte) 0x19}, new byte[]{(byte) 0x08});

        emvHandler2.setTlv(new byte[]{(byte) 0xDF, (byte) 0x81, (byte) 0x25}, ByteUtils.hexString2ByteArray("000999999999"));

        if (ByteUtils.byteArray2HexString(aid).toUpperCase().contains("A0000000043060")) {
            Log.d("nexgo",  "======maestro===== ");
            //maestro only support online pin
            emvHandler2.setTlv(new byte[]{(byte) 0x9F, (byte) 0x33}, new byte[]{(byte) 0xE0, (byte) 0x40, (byte) 0xC8});
            emvHandler2.setTlv(new byte[]{(byte) 0xDF, (byte) 0x81, (byte) 0x18}, new byte[]{(byte) 0x40});
            emvHandler2.setTlv(new byte[]{(byte) 0xDF, (byte) 0x81, (byte) 0x19}, new byte[]{(byte) 0x08});

            //set 9F1D terminal risk management - Maestro. it should be same with the MTIP configuration for 9F1D
            emvHandler2.setTlv(new byte[]{(byte) 0x9F, (byte) 0x1d}, ByteUtils.hexString2ByteArray("4C00800000000000"));
        }else{
            //set 9F1D terminal risk management - MasterCard. it should be same with the MTIP configuration for 9F1D
            emvHandler2.setTlv(new byte[]{(byte) 0x9F, (byte) 0x1d}, ByteUtils.hexString2ByteArray("6C00800000000000"));
        }


    }

    private void configExpressPayParameter(){
        //set terminal capability...
        byte[] TTC ;
        byte[] kernelTTC = emvHandler2.getTlv(ByteUtils.hexString2ByteArray("9F6E"), EmvDataSourceEnum.FROM_KERNEL);

        TTC = ByteUtils.hexString2ByteArray("D8C00000");
        kernelTTC[1] = TTC[1];

        emvHandler2.setTlv(ByteUtils.hexString2ByteArray("9F6E"), kernelTTC);

//
//        //TacDefault
//        emvHandler2.setTlv(ByteUtils.hexString2ByteArray("DF8120"), ByteUtils.hexString2ByteArray("fc50b8a000"));
//
//        //TacDecline
//        emvHandler2.setTlv(ByteUtils.hexString2ByteArray("DF8121"), ByteUtils.hexString2ByteArray("0000000000"));
//
//        //TacOnline
//        emvHandler2.setTlv(ByteUtils.hexString2ByteArray("DF8122"), ByteUtils.hexString2ByteArray("fc50808800"));
    }

    private void configPureContactlessParameter(){
        Log.d("nexgo",  "configPureContactlessParameter" );
//        emvHandler2.setPureKernelCapab(ByteUtils.hexString2ByteArray("3400400A99"));

    }

    private void configJcbContactlessParameter(){
        Log.d("nexgo",  "configJcbContactlessParameter" );

    }

    private byte[] pinRes = new byte[8];;

    private void showInputPin(boolean isOnlinPin) {
        int INJECTED_PIN_SLOT = 0;
        if (pinPad.dukptCurrentKsn(INJECTED_PIN_SLOT) == null) {
            //There is no key injected; cannot continue - show some error to user and break out
            Log.e("Nexgo", "startInputPin() : cannot continue, No key is injected!");
            emvHandler2.emvProcessCancel();  //Stop the EMV process, cannot proceed to enter PIN without the injected key
            return;
        }
        int[] pinLen = new int[]{0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c};
        pinPad.setPinKeyboardMode(PinKeyboardModeEnum.FIXED);
        //Set the PINPAD algorithm mode - we want to use DUKPT
        pinPad.setAlgorithmMode(AlgorithmModeEnum.DUKPT);
        byte[] panBytes = ByteUtils.string2ASCIIByteArray(cardNo);
        if (isOnlinPin) {
            if(cardNo == null){
                cardNo = emvHandler2.getEmvCardDataInfo().getCardNo();
            }
            pinPad.inputOnlinePin(pinLen,
                    60, cardNo.getBytes(), INJECTED_PIN_SLOT, PinAlgorithmModeEnum.ISO9564FMT1, new OnPinPadInputListener() {
                        @Override
                        public void onInputResult(int retCode, byte[] data) {
                            Log.d("nexgo", "onInputResult->:" + HexUtil.toString(data));
                            cardPin = HexUtil.toString(data);
                            Log.d("nexgo", "cardPin->:" + cardPin);

                            if (retCode == SdkResult.Success || retCode == SdkResult.PinPad_No_Pin_Input
                                    || retCode == SdkResult.PinPad_Input_Cancel) {
                                if (data != null) {
                                    byte[] temp = new byte[8];
                                    System.arraycopy(data, 0, temp, 0, 8);

                                    pinBlockArray = ByteUtils.byteArray2HexString(data).toUpperCase();                              //Set the pinBlockArray (String) to the return value 'data' (PIN output) for sending to host
                                    pinKsn = ByteUtils.byteArray2HexString(pinPad.dukptCurrentKsn(0)).toUpperCase();   //Save the pinKsn in case needed to send to host

                                    //Incremenent the KSN counter
                                    pinPad.dukptKsnIncrease(0);
                                }else {
                                    Log.d("CArd pin result", "is empty");
                                }
                                emvHandler2.onSetPinInputResponse(retCode != SdkResult.PinPad_Input_Cancel, retCode == SdkResult.PinPad_No_Pin_Input);
                            } else {
                                Log.d("nexgo", "pin enter failed");
                                emvHandler2.onSetPinInputResponse(false, false);
                            }
                        }

                        @Override
                        public void onSendKey(byte b) {}
                    });
        } else {
            pinPad.inputOfflinePin(pinLen,
                    60, new OnPinPadInputListener() {
                        @Override
                        public void onInputResult(int retCode, byte[] data) {
                            Log.d("nexgo", "onInputResult->:" + HexUtil.toString(data) + " " + retCode);

                            if (retCode == SdkResult.Success || retCode == SdkResult.PinPad_No_Pin_Input
                                    || retCode == SdkResult.PinPad_Input_Cancel) {
                                if (data != null) {
                                    byte[] temp = new byte[8];
                                    System.arraycopy(data, 0, temp, 0, 8);
                                    Log.d("data of copied pin", HexUtil.toString(temp));
                                }else {
                                    Log.d("CArd pin result", "is empty");
                                }
                                emvHandler2.onSetPinInputResponse(retCode != SdkResult.PinPad_Input_Cancel, retCode == SdkResult.PinPad_No_Pin_Input);
                            } else {
                                Log.d("nexgo", "pin enter failed");
                                emvHandler2.onSetPinInputResponse(false, false);
                            }
                        }

                        @Override
                        public void onSendKey(byte b) {}
                    });
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

    public static String byte2Char(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < bytes.length; i++) {
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
        value = (int) ( ((src[offset] & 0xFF)<<24)
                |((src[offset+1] & 0xFF)<<16)
                |((src[offset+2] & 0xFF)<<8)
                |(src[offset+3] & 0xFF));
        return value;
    }

    private void initEmvAidAndCapk() {
        Log.d("calling aid ", "init capk and aid");
        //AID

        emvHandler2.delAllAid();
        if(emvHandler2.getAidListNum() <= 0){
            List<AidEntity> aidEntityList = emvUtils.getAidList();
            Log.d("aid value", String.valueOf(aidEntityList));
            if (aidEntityList == null) {
                Log.d("nexgo", "initAID failed");
                return;
            }

            int i = emvHandler2.setAidParaList(aidEntityList);
            Log.d("nexgo", "setAidParaList " + i);
        }else{
            Log.d("nexgo", "setAidParaList " + "already load aid");
        }

        //CAPK

        emvHandler2.delAllCapk();
        int capk_num = emvHandler2.getCapkListNum();
        Log.d("nexgo", "capk_num " + capk_num);
        if(capk_num <= 0){
            List<CapkEntity> capkEntityList = emvUtils.getCapkList();
            Log.d("capk value", String.valueOf(capkEntityList));
            if (capkEntityList == null) {
                Log.d("nexgo", "initCAPK failed");
                return;
            }
            int j = emvHandler2.setCAPKList(capkEntityList);
            Log.d("nexgo", "setCAPKList " + j);
        }else{
            Log.d("nexgo", "setCAPKList " + "already load capk");
        }

    }
}

