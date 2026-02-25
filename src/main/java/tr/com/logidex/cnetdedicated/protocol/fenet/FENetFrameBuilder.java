package tr.com.logidex.cnetdedicated.protocol.fenet;

import tr.com.logidex.cnetdedicated.device.DataType;
import tr.com.logidex.cnetdedicated.device.Device;
import tr.com.logidex.cnetdedicated.device.Tag;
import tr.com.logidex.cnetdedicated.protocol.Command;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

/**
 * FENet Protocol Frame Builder for LS Electric PLC communication over TCP/IP.
 * FENet uses a binary protocol with a 20-byte header and binary data payload.
 */
public class FENetFrameBuilder {

    // FENet Header Constants
    private static final byte[] COMPANY_ID = "LSIS-XGT\0\0".getBytes(); // 10 bytes
    private static final byte[] PLC_INFO = {0x00, 0x00}; // 2 bytes - Platform info
    private static final byte CPU_INFO = 0x00; // 1 byte
    private static final byte FRAME_DIRECTION_REQUEST = 0x33; // Request
    private static final byte POSITION_INFO = 0x00; // 1 byte

    // FENet Instruction Codes
    private static final short INSTRUCTION_READ_REQUEST = 0x5400;
    private static final short INSTRUCTION_WRITE_REQUEST = 0x5800;

    // FENet Data Type Codes
    private static final byte DATA_TYPE_BIT = 0x00;
    private static final byte DATA_TYPE_BYTE = 0x01;
    private static final byte DATA_TYPE_WORD = 0x02;
    private static final byte DATA_TYPE_DWORD = 0x03;
    private static final byte DATA_TYPE_LWORD = 0x04;
    private static final byte DATA_TYPE_CONTINUOUS = 0x14;

    private int frameOrderNo = 0;
    private int invokeId = 0;

    /**
     * Build a FENet read request frame
     */
    public byte[] buildReadRequest(Tag tag, boolean isContinuous) throws IOException {
        ByteArrayOutputStream payload = new ByteArrayOutputStream();

        // Write instruction (2 bytes, little-endian)
        writeShort(payload, INSTRUCTION_READ_REQUEST);

        // Write data type (1 byte)
        payload.write(isContinuous ? DATA_TYPE_CONTINUOUS : getDataTypeCode(tag.getDataType()));

        // Reserved (1 byte)
        payload.write(0x00);

        // Number of blocks (2 bytes, little-endian)
        writeShort(payload, (short) 0x0001);

        // Length of variable name (2 bytes, little-endian)
        String varName = buildVariableName(tag);
        writeShort(payload, (short) varName.length());

        // Variable name (ASCII)
        payload.write(varName.getBytes());

        // Data count (2 bytes, little-endian) - number of items to read
        writeShort(payload, (short) 0x0001);

        return buildFrame(payload.toByteArray());
    }

    /**
     * Build a FENet write request frame
     */
    public byte[] buildWriteRequest(Tag tag, String hexValue) throws IOException {
        ByteArrayOutputStream payload = new ByteArrayOutputStream();

        // Write instruction (2 bytes, little-endian)
        writeShort(payload, INSTRUCTION_WRITE_REQUEST);

        // Write data type (1 byte)
        payload.write(getDataTypeCode(tag.getDataType()));

        // Reserved (1 byte)
        payload.write(0x00);

        // Number of blocks (2 bytes, little-endian)
        writeShort(payload, (short) 0x0001);

        // Length of variable name (2 bytes, little-endian)
        String varName = buildVariableName(tag);
        writeShort(payload, (short) varName.length());

        // Variable name (ASCII)
        payload.write(varName.getBytes());

        // Data count (2 bytes, little-endian)
        writeShort(payload, (short) 0x0001);

        // Write the data value
        byte[] dataBytes = hexStringToBytes(hexValue, tag.getDataType());
        payload.write(dataBytes);

        return buildFrame(payload.toByteArray());
    }

    /**
     * Build a FENet batch read request frame (reads multiple variables in one request)
     * FENet doesn't use registration numbers like Cnet - it reads multiple blocks directly
     */
    public byte[] buildBatchReadRequest(List<Tag> tags) throws IOException {
        ByteArrayOutputStream payload = new ByteArrayOutputStream();

        // Write instruction (2 bytes, little-endian) - Read Request
        writeShort(payload, INSTRUCTION_READ_REQUEST);

        // Write data type (1 byte) - use individual type for batch
        payload.write(0x00); // Will specify per-block

        // Reserved (1 byte)
        payload.write(0x00);

        // Number of blocks (2 bytes, little-endian) - number of variables to read
        writeShort(payload, (short) tags.size());

        // For each tag, add a read block
        for (Tag tag : tags) {
            // Length of variable name (2 bytes, little-endian)
            String varName = buildVariableName(tag);
            writeShort(payload, (short) varName.length());

            // Variable name (ASCII)
            payload.write(varName.getBytes());

            // Data count (2 bytes, little-endian) - number of items to read (1 for single value)
            writeShort(payload, (short) 0x0001);
        }

        return buildFrame(payload.toByteArray());
    }

    /**
     * Build a FENet register request frame (for monitoring)
     * Note: FENet handles batch reading differently than Cnet
     */
    public byte[] buildRegisterRequest(List<Tag> tags, String registrationNumber) throws IOException {
        // FENet doesn't use registration numbers - just do batch read directly
        return buildBatchReadRequest(tags);
    }

    /**
     * Build complete FENet frame with 20-byte header
     */
    private byte[] buildFrame(byte[] payload) throws IOException {
        ByteArrayOutputStream frame = new ByteArrayOutputStream();

        // Company ID (10 bytes) - "LSIS-XGT" + 2 null bytes
        frame.write(COMPANY_ID);

        // PLC Info (2 bytes)
        frame.write(PLC_INFO);

        // CPU Info (1 byte)
        frame.write(CPU_INFO);

        // Frame Direction (1 byte) - 0x33 for request
        frame.write(FRAME_DIRECTION_REQUEST);

        // Invoke ID / Frame Order No (2 bytes, little-endian)
        invokeId = (invokeId + 1) % 65536;
        writeShort(frame, (short) invokeId);

        // Length (2 bytes, little-endian) - length of payload
        writeShort(frame, (short) payload.length);

        // Position Info (1 byte)
        frame.write(POSITION_INFO);

        // Checksum (1 byte) - XOR of all previous bytes
        byte[] headerAndPayload = frame.toByteArray();
        byte checksum = calculateChecksum(headerAndPayload);
        frame.write(checksum);

        // Payload
        frame.write(payload);

        return frame.toByteArray();
    }

    /**
     * Calculate XOR checksum of all bytes
     */
    private byte calculateChecksum(byte[] data) {
        byte checksum = 0;
        for (byte b : data) {
            checksum ^= b;
        }
        return checksum;
    }

    /**
     * Get FENet data type code from DataType enum
     */
    private byte getDataTypeCode(DataType dataType) {
        switch (dataType) {
            case Bit:
                return DATA_TYPE_BIT;
            case Byte:
                return DATA_TYPE_BYTE;
            case Word:
                return DATA_TYPE_WORD;
            case Dword:
                return DATA_TYPE_DWORD;
            case Lword:
                return DATA_TYPE_LWORD;
            default:
                return DATA_TYPE_WORD;
        }
    }

    /**
     * Build variable name for FENet (e.g., "%MW100")
     */
    private String buildVariableName(Tag tag) {
        // Tag.toString() returns "MW100" format, we just need to prepend "%"
        return "%" + tag.toString();
    }

    /**
     * Convert hex string to bytes based on data type
     */
    private byte[] hexStringToBytes(String hexValue, DataType dataType) {
        // Remove any spaces
        hexValue = hexValue.replaceAll("\\s+", "");

        int length;
        switch (dataType) {
            case Bit:
            case Byte:
                length = 1;
                break;
            case Word:
                length = 2;
                break;
            case Dword:
                length = 4;
                break;
            case Lword:
                length = 8;
                break;
            default:
                length = 2;
        }

        // Ensure hex string is the right length
        while (hexValue.length() < length * 2) {
            hexValue = "0" + hexValue;
        }

        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            int index = i * 2;
            bytes[i] = (byte) Integer.parseInt(hexValue.substring(index, index + 2), 16);
        }

        // FENet uses little-endian for multi-byte values
        if (length > 1) {
            reverseBytes(bytes);
        }

        return bytes;
    }

    /**
     * Reverse byte array (for little-endian conversion)
     */
    private void reverseBytes(byte[] bytes) {
        for (int i = 0; i < bytes.length / 2; i++) {
            byte temp = bytes[i];
            bytes[i] = bytes[bytes.length - 1 - i];
            bytes[bytes.length - 1 - i] = temp;
        }
    }

    /**
     * Write a short value in little-endian format
     */
    private void writeShort(ByteArrayOutputStream out, short value) {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
    }

    public int getInvokeId() {
        return invokeId;
    }
}
