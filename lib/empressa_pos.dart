import 'dart:async';

import 'package:empressa_pos/card_details.dart';
import 'package:flutter/services.dart';

class EmpressaPos {
  static const MethodChannel _channel = const MethodChannel('empressa_pos');

  static Future<void> initializeHorizonTerminal() async {
    try {
      var result = await _channel.invokeMethod('initHorizonEmv');
    } catch (e) {
      print(e);
    }
  }

  static Future<dynamic> chargeBlusaltTransaction(Map<String, dynamic> normalizedTerminalData) async {
    try {
      var result = await _channel.invokeMethod('chargeBlusaltTransaction', normalizedTerminalData);
      return result;
    } catch (e) {
      print(e);
    }
  }

  static Future<dynamic> chargeTransactionFidizo(Map<String, dynamic> normalizedTerminalData) async {
    try {
      var result = await _channel.invokeMethod('chargeFidizoTransaction', normalizedTerminalData);
      return result;
    } catch (e) {
      print(e);
    }
  }

  static Future<void> stopHorizonSearch() async {
    try {
      var result = await _channel.invokeMethod('stopHorizonSearch');
    } catch (e) {
      print(e);
    }
  }

  static Future<void> horizonPrint(Map<String, dynamic> printerDetails) async {
    try {
      var result = await _channel.invokeMethod('startHorizonPrinter', printerDetails);
    } catch (e) {
      print(e);
    }
  }

  static Future<void> initializeHPos() async {
    var result ;
    try {
      result  = await _channel.invokeMethod('initializeHPos');
    } catch (e) {
      print(e);
    }
  }

  static Future<CardDetails?> horizonSearch(int transactionAmount) async {
    CardDetails? cardDetails;
    try{
      var result = await _channel
          .invokeMethod('horizonSearchCard', {"transactionAmount": transactionAmount});
      var cardResponse = Map<String, String>.from(result);
      cardDetails = CardDetails.fromJson(cardResponse);
      var track2Data = cardDetails.the57!;
      var strTrack2 = track2Data.split("F")[0];
      var pan = strTrack2.split('D')[0];
      var expiry = strTrack2.split('D')[1].substring(0, 4);
      var src = strTrack2.split("D")[1].substring(4, 7);
      cardDetails.strTrack2 = strTrack2;
      cardDetails.pan =  pan;
      cardDetails.expiry = expiry;
      cardDetails.src = src;

    } on PlatformException catch (e) {
      // cardDetails = null;
      print(e.stacktrace);
    }
    return cardDetails;
  }
}




