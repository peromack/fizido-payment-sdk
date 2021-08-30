import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:empressa_pos/pos.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  EmpressaPos.initializeTerminal();
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  CardDetails cardDetails ;

  @override
  void initState() {
    super.initState();

  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    CardDetails platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      platformVersion = await EmpressaPos.search(200);
    } on PlatformException  catch (e) {
      platformVersion = null;
      print(e.stacktrace);
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.

    setState(() {
      cardDetails = platformVersion;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Column(
          children: [
         if(cardDetails != null)  Center(
              child: Text('Running on: ${cardDetails.toJson()}\n'),
            ),
            RaisedButton(onPressed: (){
              initPlatformState();
            },
              child: Text('Search Card'),

            )
          ],
        ),
      ),
    );
  }
}