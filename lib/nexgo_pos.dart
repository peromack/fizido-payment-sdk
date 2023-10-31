import 'dart:async';

import 'package:flutter/services.dart';
import 'package:nexgo_pos/card_details.dart';

class NexgoPos {
  static const MethodChannel _channel = const MethodChannel('nexgo_pos');

  static Future<void> initializeNexgoTerminal() async {
    try {
      var result = await _channel.invokeMethod('initNexgoEmv');
      //If result is not zero, the terminate key injection failed
    } catch (e) {
      print(e);
    }
  }

  static Future<dynamic> blusaltChargeTransaction(
      Map<String, dynamic> normalizedTerminalData) async {
    try {
      var result = await _channel.invokeMethod(
          'chargeBlusaltTransaction', normalizedTerminalData);
      return result;
    } catch (e) {
      print(e);
    }
  }

  static Future<dynamic> fidizoChargeTransaction(
      Map<String, dynamic> normalizedTerminalData) async {
    try {
      var result = await _channel.invokeMethod(
          'chargeFidizoTransaction', normalizedTerminalData);
      return result;
    } catch (e) {
      print(e);
    }
  }

  static Future<void> nexgoPrint(Map<String, dynamic> printerDetails) async {
    try {
      await _channel.invokeMethod('startNexgoPrinter', printerDetails);
    } catch (e) {
      print(e);
    }
  }

  static Future<void> nexgoPrintSummary(
      Map<String, dynamic> printerDetails) async {
    try {
      await _channel.invokeMethod('startNexgoSummaryPrinter', printerDetails);
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

  static Future<bool?> checkNexgoCard() async {
    try {
      var result = await _channel.invokeMethod(
          'checkNexgoCard'); //if true there is card otherwise no card.
      return result;
    } catch (e) {
      print(e);
      return null;
    }
  }

  static Future<CardDetails?> nexgoSearch(int transactionAmount) async {
    CardDetails? cardDetails;
    try {
      var result = await _channel.invokeMethod(
          'nexgoSearchCard', {"transactionAmount": transactionAmount});
      var cardResponse = Map<String, String>.from(result);
      cardDetails = CardDetails.fromJson(cardResponse);
      var track2Data = cardDetails.the57!;
      var strTrack2 = track2Data.split("F")[0];
      var pan = strTrack2.split('D')[0];
      var expiry = strTrack2.split('D')[1].substring(0, 4);
      var src = strTrack2.split("D")[1].substring(4, 7);
      cardDetails.strTrack2 = strTrack2;
      cardDetails.pan = pan;
      cardDetails.expiry = expiry;
      cardDetails.src = src;
    } on PlatformException catch (e) {
      cardDetails = null;
      print(e.stacktrace);
    }
    return cardDetails;
  }
}
