import 'dart:async';

import 'package:analyzer_plugin/utilities/pair.dart';
import 'package:message_parser/entities/account_type.dart';
import 'package:message_parser/entities/icc_data.dart';
import 'package:message_parser/entities/original_transaction_info_data.dart';
import 'package:message_parser/entities/terminal_info.dart';
import 'package:message_parser/entities/transaction_info.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:nexgo_pos/card_details.dart';
import 'card_details.dart';

class IccUtils {
  Future<Pair<TerminalInfo, TransactionInfo>> buildTerminalData(
      {required int amount, required CardDetails cardDetails}) async {
    var nextStan = await getNextStan();
    TerminalInfo terminalInfo = TerminalInfo(
        terminalId: '2076NA61',
        merchantId: '07610000000PI91',
        merchantNameAndLocation: 'SUREPADI AGENT',
        serverIp: '',
        serverPort: 8080,
        sessionKey: '',
        countryCode: '566',
        currencyCode: '566',
        capabilities: 'E0F0C8',
        serialNumber: cardDetails.the9F1E,
        merchantCategoryCode: '7361');
    TransactionInfo transactionInfo = TransactionInfo(
        cardPIN: cardDetails.cardPin,
        pinKsn: cardDetails.ksn,
        cardPAN: cardDetails.pan,
        amount: amount,
        cardTrack2: cardDetails.strTrack2,
        csn: cardDetails.the5F34,
        accountType: AccountType.Default,
        src: cardDetails.src,
        cardExpiry: cardDetails.expiry,
        iccString: "buildIccString(cardDetails: cardDetails)",
        stan: nextStan,
        originalTransactionInfoData: OriginalTransactionInfoData(
            originalStan: nextStan,
            originalTransmissionDateAndTime: DateTime.now().toString(),
            originalAuthorizationId: nextStan,
            originalAmount: amount.toString(),
            month: '0306',
            time: -1),
        iccData: ICCData(
            transactionAmount: cardDetails.the9F02,
            anotherAmount: cardDetails.the9F03,
            authorizationRequest: cardDetails.the9F26,
            cryptogramInfoData: cardDetails.the9F27,
            issuerAppData: cardDetails.the9F10,
            unpredictableNumber: cardDetails.the9F37,
            applicationTransactionCounter: cardDetails.the9F36,
            terminalVerificationResult: cardDetails.the95,
            transactionDate: cardDetails.the9A,
            transactionType: cardDetails.the9C,
            applicationInterchangeProfile: cardDetails.the82,
            terminalCountryCode: cardDetails.the5F2A!.substring(1),
            cardholderVerificationResult: cardDetails.the9F34,
            dedicatedFileName: cardDetails.the84,
            terminalCapabilities: 'E0F0C8',
            transactionCurrencyCode: cardDetails.the5F2A,
            terminalType: cardDetails.the9F35));

    return Pair(terminalInfo, transactionInfo);
  }

  /// Generating a Unique STAN for every Transaction
  Future<String> getNextStan() async {

    await SessionManager.init();

    int newStan = 0;
    var stan = SessionManager().stanVal;
    if (stan == 0) {
      SessionManager().stanVal = 1;
    } else {
      var stanRefresh = SessionManager().stanVal;
      if (stanRefresh > 999999) {
        newStan = 0;
      } else {
        newStan = ++stan;
        SessionManager().stanVal = newStan;
      }
    }
    return newStan.toString().padLeft(6, '0');
  }
}

class SessionManager {
  static final SessionManager _sessionManager = SessionManager.internal();

  factory SessionManager() => _sessionManager;
  SessionManager.internal();

  static late SharedPreferences sharedPreferences;

  static Timer? timer;

  static Future<void> init() async {
    sharedPreferences = await SharedPreferences.getInstance();
  }

  static const String authToken = "auth_token";
  static const String phoneNumber = "phoneNumber";
  static const String deviceId = "device_id";
  static const String stan = "stan_number";
  static const String name = "logged in User Name";
  static const String fullName = "logged in user fulName";
  static const String email = "email";
  static const String address = 'address';

  set authTokenVal(String value) =>
      sharedPreferences.setString(authToken, value);
  set phoneNumberVal(String value) =>
      sharedPreferences.setString(phoneNumber, value);
  set stanVal(int value) => sharedPreferences.setInt(stan, value);
  set nameVal(String value) => sharedPreferences.setString(name, value);
  set emailVal(String value) => sharedPreferences.setString(email, value);
  set fullNameVal(String value) => sharedPreferences.setString(fullName, value);
  set addressVal(String value) => sharedPreferences.setString(address, value);

  String get authTokenVal => sharedPreferences.getString(authToken) ?? "";
  String get phoneNumberVal => sharedPreferences.getString(phoneNumber) ?? "";
  int get stanVal => sharedPreferences.getInt(stan) ?? 0;
  String get nameVal => sharedPreferences.getString(name) ?? '';
  String get emailVal => sharedPreferences.getString(email) ?? '';
  String get fullNameVal => sharedPreferences.getString(fullName) ?? '';
  String get addressVal => sharedPreferences.getString(address) ?? '';
}