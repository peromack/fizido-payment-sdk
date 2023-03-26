package com.pos.empressa.sunmi_pos.Sunmi;

import android.util.Log;

import com.pos.empressa.sunmi_pos.Sunmi.utils.ByteUtil;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

//D/Generating KEY VALUE( 3406): 7693BDCB9CB555D10000000000000000
//        D/Generating Check VALUE( 3406): AD67C6A46627956A

//D/Generating KEY VALUE( 3406): 7693BDCB9CB555D10000000000000000
//        D/Generating Check VALUE( 3406): AD67C6A46627956A

public class KeyDataAndCheckValueGenerator {

    public static byte[] deriveKey(byte[] ipek, byte[] ksn) throws Exception {
        byte[] bdk = new byte[16];
        byte[] key = new byte[16];
        byte[] checkValue = new byte[8];

        // Calculate Base Derivation Key (BDK)
        System.arraycopy(ipek, 0, bdk, 0, 8);
        System.arraycopy(ipek, 0, bdk, 8, 8);

        // Calculate the key and check value
        int keyIndex = (ksn[0] & 0x1F) << 8 | (ksn[1] & 0xFF);
        byte[] keySerialNumber = new byte[8];
        System.arraycopy(ksn, 2, keySerialNumber, 0, 6);
        keySerialNumber[6] = (byte) ((keyIndex >> 8) & 0xFF);
        keySerialNumber[7] = (byte) (keyIndex & 0xFF);

        byte[] leftKey = new byte[8];
        byte[] rightKey = new byte[8];
        byte[] mask = { (byte) 0xC0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

        for (int i = 0; i < 16; i++) {
            leftKey[i % 8] ^= mask[i % 8] & keySerialNumber[i % 8];
        }

        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(bdk, "AES");
        mac.init(keySpec);

        mac.update(leftKey);
        byte[] output = mac.doFinal();

        for (int i = 0; i < 8; i++) {
            rightKey[i] ^= mask[i] & keySerialNumber[i];
        }

        mac.update(rightKey);
        output = mac.doFinal();

        System.arraycopy(output, 0, key, 0, 8);
        System.arraycopy(output, 8, checkValue, 0, 8);

        Log.d("Generating KEY VALUE", ByteUtil.bytes2HexStr(key));
        Log.d("Generating Check VALUE", ByteUtil.bytes2HexStr(checkValue));

        byte[] keyAndCheckValue = new byte[24];
        System.arraycopy(key, 0, keyAndCheckValue, 0, 16);
        System.arraycopy(checkValue, 0, keyAndCheckValue, 16, 8);

        return keyAndCheckValue;
    }
}

