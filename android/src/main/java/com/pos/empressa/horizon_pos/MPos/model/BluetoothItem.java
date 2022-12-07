package com.pos.empressa.horizon_pos.MPos.model;

public class BluetoothItem {
    private String name;
    private String address;
    private boolean bSelected;

    public BluetoothItem(String name, String address, boolean bSelected) {
        this.name = name;
        this.address = address;
        this.bSelected = bSelected;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setSelected(boolean selected) {
        bSelected = selected;
    }

    public boolean isSelected() {
        return bSelected;
    }
}
