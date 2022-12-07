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
public enum TransactionResultCode {

    APPROVED_BY_OFFLINE,
    APPROVED_BY_ONLINE,
    DECLINED_BY_OFFLINE,
    DECLINED_BY_ONLINE,
    DECLINED_BY_TERMINAL_NEED_REVERSE,
    ERROR_TRANSCATION_CANCEL,
    ERROR_TRANSCATION_TIMEOUT,
    ERROR_UNKNOWN,
}
