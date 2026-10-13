package com.noom.meter_digital.model.modbus;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

public class ModbusDataDecoder {

    /**
     * Decode byte array into numeric value according to data type and multiplier.
     */
    public static double decodeValue(byte[] rawBytes, int offset, String dataType, double multiplier) {
        if (rawBytes == null || offset < 0 || offset >= rawBytes.length) {
            return 0.0;
        }

        String format = (dataType != null) ? dataType.toUpperCase().trim() : "FLOAT32_AB_CD";
        double rawVal = 0.0;

        switch (format) {
            case "FLOAT32_AB_CD":
                rawVal = extractFloat32_ABCD(rawBytes, offset);
                break;
            case "FLOAT32_CD_AB":
                rawVal = extractFloat32_CDAB(rawBytes, offset);
                break;
            case "FLOAT32_BA_DC":
                rawVal = extractFloat32_BADC(rawBytes, offset);
                break;
            case "FLOAT32_DC_BA":
                rawVal = extractFloat32_DCBA(rawBytes, offset);
                break;
            case "INT32_AB_CD":
                rawVal = extractInt32_ABCD(rawBytes, offset);
                break;
            case "INT32_CD_AB":
                rawVal = extractInt32_CDAB(rawBytes, offset);
                break;
            case "UINT32_AB_CD":
                rawVal = extractUInt32_ABCD(rawBytes, offset);
                break;
            case "UINT32_CD_AB":
                rawVal = extractUInt32_CDAB(rawBytes, offset);
                break;
            case "INT16":
                rawVal = extractInt16(rawBytes, offset);
                break;
            case "UINT16":
                rawVal = extractUInt16(rawBytes, offset);
                break;
            default:
                rawVal = extractFloat32_ABCD(rawBytes, offset);
                break;
        }

        if (Double.isNaN(rawVal) || Double.isInfinite(rawVal)) {
            return 0.0;
        }

        return rawVal * (multiplier != 0 ? multiplier : 1.0);
    }

    public static float extractFloat32_ABCD(byte[] bytes, int offset) {
        if (offset + 3 >= bytes.length) return 0.0f;
        ByteBuffer bb = ByteBuffer.wrap(bytes, offset, 4);
        bb.order(ByteOrder.BIG_ENDIAN);
        float val = bb.getFloat();
        return (Float.isNaN(val) || Float.isInfinite(val)) ? 0.0f : val;
    }

    public static float extractFloat32_CDAB(byte[] bytes, int offset) {
        if (offset + 3 >= bytes.length) return 0.0f;
        byte[] swapped = new byte[]{bytes[offset + 2], bytes[offset + 3], bytes[offset], bytes[offset + 1]};
        ByteBuffer bb = ByteBuffer.wrap(swapped);
        bb.order(ByteOrder.BIG_ENDIAN);
        float val = bb.getFloat();
        return (Float.isNaN(val) || Float.isInfinite(val)) ? 0.0f : val;
    }

    public static float extractFloat32_BADC(byte[] bytes, int offset) {
        if (offset + 3 >= bytes.length) return 0.0f;
        byte[] swapped = new byte[]{bytes[offset + 1], bytes[offset], bytes[offset + 3], bytes[offset + 2]};
        ByteBuffer bb = ByteBuffer.wrap(swapped);
        bb.order(ByteOrder.BIG_ENDIAN);
        float val = bb.getFloat();
        return (Float.isNaN(val) || Float.isInfinite(val)) ? 0.0f : val;
    }

    public static float extractFloat32_DCBA(byte[] bytes, int offset) {
        if (offset + 3 >= bytes.length) return 0.0f;
        byte[] swapped = new byte[]{bytes[offset + 3], bytes[offset + 2], bytes[offset + 1], bytes[offset]};
        ByteBuffer bb = ByteBuffer.wrap(swapped);
        bb.order(ByteOrder.BIG_ENDIAN);
        float val = bb.getFloat();
        return (Float.isNaN(val) || Float.isInfinite(val)) ? 0.0f : val;
    }

    public static int extractInt32_ABCD(byte[] bytes, int offset) {
        if (offset + 3 >= bytes.length) return 0;
        return ((bytes[offset] & 0xFF) << 24) |
               ((bytes[offset + 1] & 0xFF) << 16) |
               ((bytes[offset + 2] & 0xFF) << 8) |
               (bytes[offset + 3] & 0xFF);
    }

    public static int extractInt32_CDAB(byte[] bytes, int offset) {
        if (offset + 3 >= bytes.length) return 0;
        return ((bytes[offset + 2] & 0xFF) << 24) |
               ((bytes[offset + 3] & 0xFF) << 16) |
               ((bytes[offset] & 0xFF) << 8) |
               (bytes[offset + 1] & 0xFF);
    }

    public static long extractUInt32_ABCD(byte[] bytes, int offset) {
        return extractInt32_ABCD(bytes, offset) & 0xFFFFFFFFL;
    }

    public static long extractUInt32_CDAB(byte[] bytes, int offset) {
        return extractInt32_CDAB(bytes, offset) & 0xFFFFFFFFL;
    }

    public static short extractInt16(byte[] bytes, int offset) {
        if (offset + 1 >= bytes.length) return 0;
        return (short) (((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF));
    }

    public static int extractUInt16(byte[] bytes, int offset) {
        if (offset + 1 >= bytes.length) return 0;
        return ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
    }

    /**
     * Interpret a 4-byte block at specified byte offset in ALL formats simultaneously.
     * Useful for remote engineers trying to discover how unknown meters encode their values.
     */
    public static Map<String, Object> interpret4Bytes(byte[] bytes, int byteOffset, int startRegisterIndex) {
        Map<String, Object> result = new HashMap<>();
        result.put("byteOffset", byteOffset);
        result.put("registerIndex", startRegisterIndex + (byteOffset / 2));

        if (bytes == null || byteOffset + 3 >= bytes.length) {
            result.put("valid", false);
            return result;
        }

        result.put("valid", true);
        result.put("hex", String.format("%02X %02X %02X %02X",
                bytes[byteOffset] & 0xFF,
                bytes[byteOffset + 1] & 0xFF,
                bytes[byteOffset + 2] & 0xFF,
                bytes[byteOffset + 3] & 0xFF));

        float floatABCD = extractFloat32_ABCD(bytes, byteOffset);
        float floatCDAB = extractFloat32_CDAB(bytes, byteOffset);
        float floatBADC = extractFloat32_BADC(bytes, byteOffset);
        float floatDCBA = extractFloat32_DCBA(bytes, byteOffset);

        result.put("float_ABCD", round4(floatABCD));
        result.put("float_CDAB", round4(floatCDAB));
        result.put("float_BADC", round4(floatBADC));
        result.put("float_DCBA", round4(floatDCBA));

        result.put("int32_ABCD", extractInt32_ABCD(bytes, byteOffset));
        result.put("int32_CDAB", extractInt32_CDAB(bytes, byteOffset));
        result.put("uint32_ABCD", extractUInt32_ABCD(bytes, byteOffset));
        result.put("uint32_CDAB", extractUInt32_CDAB(bytes, byteOffset));

        result.put("uint16_reg0", extractUInt16(bytes, byteOffset));
        result.put("uint16_reg1", extractUInt16(bytes, byteOffset + 2));

        return result;
    }

    private static double round4(double val) {
        return Math.round(val * 10000.0) / 10000.0;
    }
}
