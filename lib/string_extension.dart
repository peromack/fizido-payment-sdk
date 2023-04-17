import 'dart:typed_data';

import 'package:flutter/services.dart';

extension AssetPngStringToBitmap on String {
  static const defaultImagePath = "assets/images/fizido-logo.png";

  Future<Uint8List> toBitmap() async {
    try {
      final data = await rootBundle.load(this);
      final bytes = data.buffer.asUint8List();
      return bytes;
    } catch (_) {
      final data = await rootBundle.load(defaultImagePath);
      final bytes = data.buffer.asUint8List();
      return bytes;
    }
  }
}