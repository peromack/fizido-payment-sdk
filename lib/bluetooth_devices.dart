
class BluetoothDevices {
  BluetoothDevices({
    this.address,
    this.bSelected,
    this.name,
  });

  final String address;
  final bool bSelected;
  final String name;

  factory BluetoothDevices.fromJson(Map<String, dynamic> json) => BluetoothDevices(
    address: json['address'],
    bSelected: json['bSelected'],
    name: json['name'],
  );

  Map<String, dynamic> toJson() => {
    'address': address,
    'bSelected': bSelected,
    'name': name,
  };
}
