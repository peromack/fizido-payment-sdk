
import 'dart:async';

import 'package:empressa_pos/card_details.dart';
import 'package:flutter/services.dart';

class EmpressaPos {
  static const MethodChannel _channel =
      const MethodChannel('empressa_pos');

  static Future<Map<String,String>> get platformVersion async {
    final Map<String,String> version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<CardDetails> search() async {
    CardDetails cardDetails ;
    try {
        var result = await _channel.invokeMethod('searchCard');
        print(result);
       var cardResponse = Map<String, String>.from(result) ;
        cardDetails = CardDetails.fromJson(cardResponse);

        var track2Data = cardDetails.the57 ;
        var strTrack2 = track2Data.split("F")[0] ;
        var pan = strTrack2.split('D')[0] ;
        var expiry = strTrack2.split("D")[1].substring(0,4);
        var src = strTrack2.split("D")[1].substring(4,7);
        cardDetails.strTrack2 = strTrack2 ;
        cardDetails.pan = pan ;
        cardDetails.expiry = expiry ;
        cardDetails.src = src ;
    } on PlatformException catch (e) {
      cardDetails = null ;
      print(e.stacktrace);
    }
return cardDetails;
  }
}
