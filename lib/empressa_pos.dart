import 'dart:async';
import 'dart:convert';

import 'package:empressa_pos/bluetooth_devices.dart';
import 'package:empressa_pos/card_details.dart';
import 'package:flutter/services.dart';

class EmpressaPos {
  static const MethodChannel _channel = const MethodChannel('empressa_pos');

  // static Future<CardDetails> search(int transactionAmount) async {
  //   late CardDetails cardDetails;
  //   try {
  //     var result = await _channel
  //         .invokeMethod('searchCard', {"transactionAmount": transactionAmount});
  //     print(result);
  //     var cardResponse = Map<String, String>.from(result);
  //     cardDetails = CardDetails.fromJson(cardResponse);
  //     var track2Data = cardDetails.the57;
  //     var strTrack2 = track2Data!.split("F")[0];
  //     var pan = strTrack2.split('D')[0];
  //     var expiry = strTrack2.split('D')[1].substring(0, 4);
  //     var src = strTrack2.split("D")[1].substring(4, 7);
  //     cardDetails.strTrack2 = strTrack2;
  //     cardDetails.pan =  pan;
  //     cardDetails.expiry = expiry;
  //     cardDetails.src = src;
  //   } on PlatformException catch (e) {
  //    // cardDetails = null;
  //     print(e.stacktrace);
  //   }
  //   return cardDetails;
  // }

  static Future<void> initializeTerminal() async {
    try {
      var result = await _channel.invokeMethod('initEmv');
    } catch (e) {
      print(e);
    }
  }

  static Future<void> initializeHorizonTerminal() async {
    try {
      var result = await _channel.invokeMethod('initHorizonEmv');
    } catch (e) {
      print(e);
    }
  }

  static Future<void> sunyardChargeTransaction(Map<String, dynamic> normalizedTerminalData) async {
    try {
      var result = await _channel.invokeMethod('chargeSunyardTransaction', normalizedTerminalData);
      return result;
    } catch (e) {
      print(e);
    }
  }

  static Future<void> sunyardChargeTransactionFidizo(Map<String, dynamic> normalizedTerminalData) async {
    try {
      var result = await _channel.invokeMethod('chargeSunyardFidizoTransaction', normalizedTerminalData);
      return result;
    } catch (e) {
      print(e);
    }
  }

  // static Future<void> sunyardChargeTransaction(Map<String, dynamic> requestBodyDetails) async {
  //   try {
  //     var result = await _channel.invokeMethod('chargeSunyardTransaction', requestBodyDetails);
  //     return result;
  //   } catch (e) {
  //     print(e);
  //   }
  // }

  static Future<void> stopSearch() async {
    try {
      var result = await _channel.invokeMethod('stopSearch');
    } catch (e) {
      print(e);
    }
  }

  static Future<void> sunyardPrint(Map<String, dynamic> printerDetails) async {
    try {
      var result = await _channel.invokeMethod('startPrinter', printerDetails);
    } catch (e) {
      print(e);
    }
  }

  static Future<bool> checkCard() async {
    var result ;
    try {
       result  = await _channel.invokeMethod('checkSunyardCard');

    } catch (e) {
      print(e);
    }
    return result ;
  }

  static Future<void> initializeMPos() async {
    var result ;
    try {
      result  = await _channel.invokeMethod('initializeMPos');
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


  static Future<CardDetails?> search(int transactionAmount) async {
    CardDetails? cardDetails;
    try {
      var result = await _channel
          .invokeMethod('searchCard', {"transactionAmount": transactionAmount});
      print(result);
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




