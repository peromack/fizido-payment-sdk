package com.pos.empressa.horizon_pos.Horizon.pay;
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
public class OnlineRespEntitiy {
    String respCode;
    String iccData;

    public String getRespCode() {
        return respCode;
    }

    public void setRespCode(String respCode) {
        this.respCode = respCode;
    }

    public String getIccData() {
        return iccData;
    }

    public void setIccData(String iccData) {
        this.iccData = iccData;
    }
}
