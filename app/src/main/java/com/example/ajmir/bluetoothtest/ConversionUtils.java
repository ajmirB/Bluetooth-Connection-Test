package com.example.ajmir.bluetoothtest;

import android.support.annotation.NonNull;
import android.util.Log;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

/**
 * Utils class to help with conversion of Bytes to Hex.
 */
public final class ConversionUtils {

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    /**
     * Convert major or minor to hex byte[]. This is used to create a {@link android.bluetooth.le.ScanFilter}.
     *
     * @param value major or minor to convert to byte[]
     * @return byte[]
     */
    public static byte[] integerToByteArray(final int value) {
        final ByteBuffer bb = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(value);
        return bb.array();
    }

    /**
     * Convert major and minor byte array to integer.
     *
     * @param byteArray that contains major and minor byte
     * @return integer value for major and minor
     */
    public static int byteArrayToInteger(final byte[] byteArray) {
        ByteBuffer wrapped = ByteBuffer.wrap(byteArray); // big-endian by default
        wrapped.order(ByteOrder.LITTLE_ENDIAN);
        return wrapped.getInt();
    }
}
