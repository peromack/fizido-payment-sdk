import 'dart:async';

import 'package:sunmi_pos/card_details.dart';
import 'package:flutter/services.dart';

class NexgoPos {
  static const MethodChannel _channel = const MethodChannel('sunmi_pos');

  static Future<void> initializeNexgoTerminal() async {
    try {
      var result = await _channel.invokeMethod('initNexgoEmv');
    } catch (e) {
      print(e);
    }
  }

  static Future<dynamic> blusaltChargeTransaction(Map<String, dynamic> normalizedTerminalData) async {
    try {
      var result = await _channel.invokeMethod('chargeBlusaltTransaction', normalizedTerminalData);
      return result;
    } catch (e) {
      print(e);
    }
  }

  static Future<dynamic> fidizoChargeTransaction(Map<String, dynamic> normalizedTerminalData) async {
    try {
      var result = await _channel.invokeMethod('chargeFidizoTransaction', normalizedTerminalData);
      return result;
    } catch (e) {
      print(e);
    }
  }

  static Future<void> nexgoPrint(Map<String, dynamic> printerDetails) async {
    try {
      var result = await _channel.invokeMethod('startNexgoPrinter', printerDetails);
    } catch (e) {
      print(e);
    }
  }

  static Future<void> nexgoCancelSearch() async {
    try {
      var result = await _channel.invokeMethod('cancelNexgoSearch');
      return result;
    } catch (e) {
      print(e);
    }
  }

  static Future<CardDetails?> nexgoSearch(int transactionAmount) async {
    CardDetails? cardDetails;
    try{
      var result = await _channel
          .invokeMethod('nexgoSearchCard', {"transactionAmount": transactionAmount});
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
      cardDetails = null;
      print(e.stacktrace);
    }
    return cardDetails;
  }
}




