import 'package:empressa_pos/bluetooth_devices.dart';
import 'package:empressa_pos/icc.dart';
import 'package:flutter/material.dart';
import 'dart:async';
import 'package:analyzer_plugin/utilities/pair.dart';
import 'package:message_parser/entities/terminal_info.dart';
import 'package:message_parser/entities/transaction_info.dart';

import 'package:flutter/services.dart';
import 'package:empressa_pos/pos.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  EmpressaPos.initializeMPos();
  EmpressaPos.initializeTerminal();
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  List<BluetoothDevices> bluetoothDevices ;
  bool connectionResult ;
  CardDetails cardDetails;

  @override
  void initState() {
    super.initState();

  }

  Future<void> chargeTransaction() async {

    Pair<TerminalInfo, TransactionInfo> terminalData = await IccUtils().buildTerminalData(amount: 10, cardDetails: cardDetails);

    Map<String, dynamic> normalizedTerminalData = new Map();

    Map<String, dynamic> firstData = terminalData.first.toJson();
    Map<String, dynamic> secondData = terminalData.last.toJson();

    Map<String, dynamic> iccData = secondData["iccData"];

    Map<String, dynamic> orgTransData = secondData["originalTransactionInfoData"];

    secondData.remove("iccData");
    secondData.remove("originalTransactionInfoData");

    normalizedTerminalData.addAll(firstData);
    normalizedTerminalData.addAll(secondData);
    normalizedTerminalData.addAll(iccData);
    normalizedTerminalData.addAll(orgTransData);

    try {

      await EmpressaPos.sunyardChargeTransaction(normalizedTerminalData);
      setState(() {

      });
    } on PlatformException  catch (e) {
      print(e);
    }
  }

  Future<void> searchSunyard() async {

    try {

      cardDetails = await EmpressaPos.search(100);
      setState(() {

      });
    } on PlatformException  catch (e) {
      print(e);
    }
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
            RaisedButton(onPressed: (){
              searchSunyard();
            },
              child: Text('Search Atm Card'),

            ),
            RaisedButton(onPressed: (){
              chargeTransaction();
            },
              child: Text('Pay Charge'),

            ),
            SizedBox(height: 20,),
            Expanded(child: ListView.separated(
              shrinkWrap: true,
              itemCount:  bluetoothDevices == null ? 0 :bluetoothDevices.length,
              itemBuilder: (BuildContext context, int index) {
                return InkWell(
                  onTap: (){
                    // connectDevices(bluetoothName: bluetoothDevices[index].name,bluetoothMac: bluetoothDevices[index].address);
                  },
                    child: Text('${bluetoothDevices[index].name + bluetoothDevices[index].address}'));
              }, separatorBuilder: (BuildContext context, int index) {
                return SizedBox(height: 20,);
            },)),
            Text(connectionResult == true ? 'Connected' : 'I NO Fit CONNECT' + '$connectionResult')
          ],
        ),
      ),
    );
  }
}