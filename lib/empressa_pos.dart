
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


    } on PlatformException catch (e) {
      cardDetails = null ;
      print(e.stacktrace);
    }
return cardDetails;
  }
}
