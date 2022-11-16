package com.pos.empressa.nexgo_pos.MPos.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

public class BytesUtil {

    private static byte[] getBytes(byte data) {
        byte[] bytes = new byte[1];
        bytes[0] = data;
        return bytes;
    }

    private static byte[] getBytes(String data, String charsetName) {
        Charset charset = Charset.forName(charsetName);
        return data.getBytes(charset);
    }

    public static byte[] getBytes(String data) {
        return getBytes(data, "GBK");
    }

    private static byte getByte(byte[] bytes) {
        return bytes[0];
    }

    private static String getString(byte[] bytes, String charsetName) {
        return new String(bytes, Charset.forName(charsetName));
    }

    private static String getString(byte[] bytes) {
        return getString(bytes, "GBK");
    }

    public static byte[] hexString2ByteArray(String hexStr) {
        if (hexStr == null)
            return null;
        if (hexStr.length() % 2 != 0) {
            return null;
        }
        byte[] data = new byte[hexStr.length() / 2];
        for (int i = 0; i < hexStr.length() / 2; i++) {
            char hc = hexStr.charAt(2 * i);
            char lc = hexStr.charAt(2 * i + 1);
            byte hb = hexChar2Byte(hc);
            byte lb = hexChar2Byte(lc);
            if ((hb < 0) || (lb < 0)) {
                return null;
            }
            int n = hb << 4;
            data[i] = (byte) (n + lb);
        }
        return data;
    }

    public static byte hexChar2Byte(char c) {
        if ((c >= '0') && (c <= '9'))
            return (byte) (c - '0');
        if ((c >= 'a') && (c <= 'f'))
            return (byte) (c - 'a' + 10);
        if ((c >= 'A') && (c <= 'F'))
            return (byte) (c - 'A' + 10);
        return -1;
    }

    public static byte[] streamCopy(List<byte[]> srcArrays) {
        byte[] destAray = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            for (byte[] srcArray : srcArrays) {
                bos.write(srcArray);
            }
            bos.flush();
            destAray = bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bos.close();
            } catch (IOException e) {
            }
        }
        return destAray;
    }

    public static String getString(Map<String, Object> map, String key) {
        return getString((byte[]) map.get(key));
    }

    public static byte[] getBytes(Map<String, Object> map, String key) {
        return (byte[]) map.get(key);
    }

    public static byte getByte(Map<String, Object> map, String key) {
        return getByte((byte[]) map.get(key));
    }

    public static void putString(Map<String, Object> map, String key, String s) {
        map.put(key, getBytes(s));
    }

    public static void putBytes(Map<String, Object> map, String key, byte[] b) {
        map.put(key, b);
    }

    public static void putByte(Map<String, Object> map, String key, byte b) {
        map.put(key, getBytes(b));
    }

    public static String printHex(byte[] b, int len) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < len; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            builder.append(hex.toUpperCase() + " ");
        }
        return builder.toString();
    }


    public static String bytes2Hex(byte[] b) {
        if (b == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            builder.append(hex.toUpperCase());
        }
        return builder.toString();
    }

    public static byte[] subBytes(byte[] src, int begin, int count) {
        byte[] bs = new byte[count];
        System.arraycopy(src, begin, bs, 0, count);
        return bs;
    }

    public static byte[] conver(ByteBuffer byteBuffer) {
        int len = byteBuffer.limit() - byteBuffer.position();
        byte[] bytes = new byte[len];

        if (byteBuffer.isReadOnly()) {
            return null;
        } else {
            byteBuffer.get(bytes);
        }
        return bytes;
    }

    public static byte[] merge(byte[]... data) {
        if (data == null) {
            return null;
        } else {
            byte[] bytes = null;

            for (int i = 0; i < data.length; ++i) {
                bytes = mergeBytes(bytes, data[i]);
            }

            return bytes;
        }
    }

    public static byte[] mergeBytes(byte[] bytesA, byte[] bytesB) {
        if (bytesA != null && bytesA.length != 0) {
            if (bytesB != null && bytesB.length != 0) {
                byte[] bytes = new byte[bytesA.length + bytesB.length];
                System.arraycopy(bytesA, 0, bytes, 0, bytesA.length);
                System.arraycopy(bytesB, 0, bytes, bytesA.length, bytesB.length);
                return bytes;
            } else {
                return bytesA;
            }
        } else {
            return bytesB;
        }
    }

    public static String getReadableAmount(String amount) {
        if (amount != null && !amount.isEmpty()) {
            DecimalFormat df = new DecimalFormat("0.00");
            return df.format(Double.parseDouble(amount) / 100.0D);
        } else {
            return "0.00";
        }
    }

    /**
     * 隐藏字符
     *
     * @param value
     * @param mask
     * @return
     */
    public static String mask(String value, String mask) {
        int markStart = -1;
        for (int i = 0; i < mask.length(); i++) {
            if (mask.charAt(i) == '*') {
                markStart = i;
                break;
            }
            if (mask.charAt(i) != 'x' && mask.charAt(i) != 'X') {
                markStart = i;
                break;
            }
        }

        if (markStart == -1 || markStart >= value.length()) {
            return value;
        }

        int markEnd = markStart + 1;
        for (int i = mask.length() - 1; i > markStart + 1; i--) {
            if (mask.charAt(i) == '*') {
                markEnd = i + 1;
                break;
            }
            if (mask.charAt(i) != 'x' && mask.charAt(i) != 'X') {
                markEnd = i + 1;
                break;
            }
        }

        if (mask.length() - markEnd + markStart >= value.length()) {
            return value;
        }

        StringBuilder str = new StringBuilder();
        str.append(value.substring(0, markStart));

        markEnd += value.length() - mask.length();

        for (int i = markStart; i < markEnd; i++) {
            str.append("*");
        }

        if (markEnd < value.length()) {
            str.append(value.substring(markEnd));
        }

        return str.toString();
    }

    public static final byte[] input2byte(InputStream inStream)
            throws IOException {
        ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
        byte[] buff = new byte[256];
        int rc = 0;
        while ((rc = inStream.read(buff, 0, buff.length)) > 0) {
            swapStream.write(buff, 0, rc);
        }
        byte[] bytes = swapStream.toByteArray();
        return bytes;
    }

}
