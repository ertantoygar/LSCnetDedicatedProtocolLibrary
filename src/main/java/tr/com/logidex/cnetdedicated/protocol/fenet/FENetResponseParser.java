package tr.com.logidex.cnetdedicated.protocol.fenet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Parser for FENet protocol responses from LS Electric PLC
 */
public class FENetResponseParser {

    private static final int HEADER_LENGTH = 20;
    private static final byte FRAME_DIRECTION_RESPONSE = 0x11;

    /**
     * Parse a complete FENet response from byte array
     */
    public FENetResponse parse(byte[] responseBytes) {
        FENetResponse response = new FENetResponse();

        try {
            if (responseBytes == null || responseBytes.length < HEADER_LENGTH) {
                response.setValid(false);
                response.setErrorMessage("Response too short: " + (responseBytes != null ? responseBytes.length : 0) + " bytes");
                return response;
            }

            ByteArrayInputStream stream = new ByteArrayInputStream(responseBytes);

            // Parse 20-byte header
            parseHeader(stream, response);

            // Verify checksum
            if (!verifyChecksum(responseBytes)) {
                response.setValid(false);
                response.setErrorMessage("Checksum verification failed");
                return response;
            }

            // Parse payload
            parsePayload(stream, response);

            response.setValid(true);

        } catch (Exception e) {
            response.setValid(false);
            response.setErrorMessage("Parse error: " + e.getMessage());
        }

        return response;
    }

    /**
     * Parse the 20-byte FENet header
     */
    private void parseHeader(ByteArrayInputStream stream, FENetResponse response) throws IOException {
        // Company ID (10 bytes) - should be "LSIS-XGT\0\0"
        byte[] companyId = new byte[10];
        stream.read(companyId);
        response.setCompanyId(companyId);

        // PLC Info (2 bytes)
        byte[] plcInfo = new byte[2];
        stream.read(plcInfo);
        response.setPlcInfo(plcInfo);

        // CPU Info (1 byte)
        response.setCpuInfo((byte) stream.read());

        // Frame Direction (1 byte) - should be 0x11 for response
        response.setFrameDirection((byte) stream.read());

        // Invoke ID (2 bytes, little-endian)
        int invokeId = readShortLE(stream);
        response.setInvokeId(invokeId);

        // Length (2 bytes, little-endian)
        int length = readShortLE(stream);
        response.setLength(length);

        // Position Info (1 byte)
        response.setPositionInfo((byte) stream.read());

        // Checksum (1 byte)
        response.setChecksum((byte) stream.read());
    }

    /**
     * Parse the payload section
     */
    private void parsePayload(ByteArrayInputStream stream, FENetResponse response) throws IOException {
        // Instruction (2 bytes, little-endian)
        short instruction = (short) readShortLE(stream);
        response.setInstruction(instruction);

        // Data Type (1 byte)
        response.setDataType((byte) stream.read());

        // Reserved (1 byte)
        response.setReserved((byte) stream.read());

        // Number of Blocks (2 bytes, little-endian)
        short numberOfBlocks = (short) readShortLE(stream);
        response.setNumberOfBlocks(numberOfBlocks);

        if (response.isReadResponse()) {
            parseReadResponse(stream, response);
        } else if (response.isWriteResponse()) {
            parseWriteResponse(stream, response);
        }
    }

    /**
     * Parse read response payload
     */
    private void parseReadResponse(ByteArrayInputStream stream, FENetResponse response) throws IOException {
        // For read response, we need to handle both single and multi-block responses
        int numberOfBlocks = response.getNumberOfBlocks();

        if (numberOfBlocks == 1) {
            // Single block read
            parseSingleBlockRead(stream, response);
        } else if (numberOfBlocks > 1) {
            // Multi-block read (batch read)
            parseMultiBlockRead(stream, response, numberOfBlocks);
        }
    }

    /**
     * Parse single block read response
     */
    private void parseSingleBlockRead(ByteArrayInputStream stream, FENetResponse response) throws IOException {
        // Block Number (2 bytes, little-endian)
        readShortLE(stream);

        // Data Length (2 bytes, little-endian)
        int dataLength = readShortLE(stream);

        // Read remaining bytes as data
        byte[] data = new byte[stream.available()];
        stream.read(data);

        // Check if this is an error response (typically very short)
        if (data.length == 2) {
            // Might be an error status
            short errorStatus = (short) ((data[0] & 0xFF) | ((data[1] & 0xFF) << 8));
            response.setErrorStatus(errorStatus);
            response.setData(new byte[0]);
        } else {
            // Success - set data
            response.setErrorStatus((short) 0x0000);
            response.setData(data);
        }
    }

    /**
     * Parse multi-block read response (for batch reads)
     */
    private void parseMultiBlockRead(ByteArrayInputStream stream, FENetResponse response, int numberOfBlocks) throws IOException {
        // For batch reads, we'll store all data blocks concatenated
        // The caller will need to split them based on the number of tags
        ByteArrayOutputStream allData = new ByteArrayOutputStream();

        for (int i = 0; i < numberOfBlocks; i++) {
            // Block Number (2 bytes, little-endian)
            readShortLE(stream);

            // Data Length (2 bytes, little-endian)
            int dataLength = readShortLE(stream);

            // Data (variable length based on dataLength)
            byte[] blockData = new byte[dataLength];
            stream.read(blockData);
            allData.write(blockData);
        }

        response.setErrorStatus((short) 0x0000);
        response.setData(allData.toByteArray());
    }

    /**
     * Parse write response payload
     */
    private void parseWriteResponse(ByteArrayInputStream stream, FENetResponse response) throws IOException {
        // For write response:
        // - Error Status (2 bytes)

        if (stream.available() >= 2) {
            short errorStatus = (short) readShortLE(stream);
            response.setErrorStatus(errorStatus);
        } else {
            response.setErrorStatus((short) 0x0000); // Assume success if no error code
        }

        response.setData(new byte[0]);
    }

    /**
     * Read a 16-bit short value in little-endian format
     */
    private int readShortLE(ByteArrayInputStream stream) throws IOException {
        int low = stream.read() & 0xFF;
        int high = stream.read() & 0xFF;
        return (high << 8) | low;
    }

    /**
     * Verify the checksum of the response
     */
    private boolean verifyChecksum(byte[] data) {
        if (data.length < HEADER_LENGTH) {
            return false;
        }

        byte calculatedChecksum = 0;
        // XOR all bytes except the checksum itself (byte 19)
        for (int i = 0; i < 19; i++) {
            calculatedChecksum ^= data[i];
        }

        byte receivedChecksum = data[19];
        return calculatedChecksum == receivedChecksum;
    }

    /**
     * Helper method to convert byte array to hex string for debugging
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "";
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02X ", b));
        }
        return hex.toString().trim();
    }
}
