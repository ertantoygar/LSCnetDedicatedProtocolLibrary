package tr.com.logidex.cnetdedicated.protocol;

import tr.com.logidex.cnetdedicated.device.DataType;
import tr.com.logidex.cnetdedicated.device.Tag;
import tr.com.logidex.cnetdedicated.protocol.fenet.FENetFrameBuilder;
import tr.com.logidex.cnetdedicated.protocol.fenet.FENetResponse;
import tr.com.logidex.cnetdedicated.protocol.fenet.FENetResponseParser;

import java.io.IOException;
import java.util.List;

/**
 * FENet Protocol Handler for TCP/IP communication
 * Uses binary protocol with 20-byte header
 */
public class FENetProtocolHandler implements ProtocolHandler {

    private final FENetFrameBuilder frameBuilder;
    private final FENetResponseParser responseParser;

    public FENetProtocolHandler() {
        this.frameBuilder = new FENetFrameBuilder();
        this.responseParser = new FENetResponseParser();
    }

    @Override
    public Object buildReadRequest(Tag tag, CommandType commandType) throws IOException {
        boolean isContinuous = (commandType == CommandType.SB || commandType == CommandType.RSB);
        return frameBuilder.buildReadRequest(tag, isContinuous);
    }

    @Override
    public Object buildWriteRequest(Tag tag, CommandType commandType) throws IOException {
        if (tag.getDataType() != DataType.Word) {
            throw new IllegalArgumentException("This tag type is not a Word type!");
        }
        return frameBuilder.buildWriteRequest(tag, tag.getValueAsHexString());
    }

    @Override
    public Object buildWriteDoubleRequest(Tag tag) throws IOException {
        if (tag.getDataType() != DataType.Dword) {
            throw new IllegalArgumentException("This tag type is not a DoubleWord type!");
        }
        return frameBuilder.buildWriteRequest(tag, tag.getValueAsHexString());
    }

    @Override
    public Object buildWriteBitRequest(Tag tag, boolean value) throws IOException {
        if (tag.getDataType() != DataType.Bit) {
            throw new IllegalArgumentException("This tag type is not a Bit type!");
        }
        String hexValue = value ? "01" : "00";
        return frameBuilder.buildWriteRequest(tag, hexValue);
    }

    @Override
    public Object buildWriteStringRequest(Tag tag, String strData, int theLimitToWrite) throws IOException {
        if ((tag.getDataType() != DataType.Word)) {
            throw new IllegalArgumentException("The tag's data type must be a word when writing a string!");
        }
        if (theLimitToWrite > 64) {
            throw new IllegalArgumentException("The parameter theLimitToWrite must not be greater than 64!");
        }

        // Prepare string data
        if (strData.length() > theLimitToWrite) {
            strData = strData.substring(0, theLimitToWrite);
        }
        while (strData.length() < theLimitToWrite) {
            strData += " ";
        }

        // Convert string to hex
        StringBuilder hexData = new StringBuilder();
        for (char c : strData.toCharArray()) {
            hexData.append(String.format("%02X", (int) c));
        }

        return frameBuilder.buildWriteRequest(tag, hexData.toString());
    }

    @Override
    public Object buildRegisterRequest(List<Tag> tags, String registrationNumber) throws IOException {
        // FENet doesn't use registration numbers - it does batch reads directly
        // We'll build the batch read request and return it
        return frameBuilder.buildBatchReadRequest(tags);
    }

    @Override
    public Object buildExecuteRequest(String registrationNumber) throws IOException {
        // FENet doesn't have a separate execute step
        // The batch read is done in one request during "register"
        // This method won't be called for FENet, but we implement it for interface compliance
        throw new UnsupportedOperationException("FENet doesn't use separate execute - batch read is done in registerDevicesToMonitor");
    }

    @Override
    public String parseResponse(Object rawResponse) throws Exception {
        if (!(rawResponse instanceof byte[])) {
            throw new IllegalArgumentException("FENet expects byte[] response");
        }

        byte[] responseBytes = (byte[]) rawResponse;
        FENetResponse response = responseParser.parse(responseBytes);

        if (!response.isValid()) {
            throw new Exception("Invalid FENet response: " + response.getErrorMessage());
        }

        if (!response.isSuccess()) {
            throw new Exception("FENet error status: 0x" + String.format("%04X", response.getErrorStatus()));
        }

        return response.getDataAsHexString();
    }

    @Override
    public boolean isResponseValid(Object rawResponse) {
        if (!(rawResponse instanceof byte[])) {
            return false;
        }

        byte[] responseBytes = (byte[]) rawResponse;
        FENetResponse response = responseParser.parse(responseBytes);
        return response.isValid() && response.isSuccess();
    }

    @Override
    public Command getCommandFromResponse(Object rawResponse) {
        if (!(rawResponse instanceof byte[])) {
            return null;
        }

        byte[] responseBytes = (byte[]) rawResponse;
        FENetResponse response = responseParser.parse(responseBytes);
        return response.getCommand();
    }

    @Override
    public String getStructuredDataArea(Object rawResponse) {
        if (!(rawResponse instanceof byte[])) {
            return "";
        }

        byte[] responseBytes = (byte[]) rawResponse;
        FENetResponse response = responseParser.parse(responseBytes);
        return response.getDataAsHexString();
    }

    @Override
    public boolean isBinaryProtocol() {
        return true;
    }

    /**
     * Parse batch read response and extract data for multiple tags
     * @param rawResponse The raw byte[] response
     * @param tags The list of tags that were read
     * @return Array of hex strings, one for each tag
     */
    public String[] parseBatchReadResponse(Object rawResponse, List<tr.com.logidex.cnetdedicated.device.Tag> tags) throws Exception {
        if (!(rawResponse instanceof byte[])) {
            throw new IllegalArgumentException("FENet expects byte[] response");
        }

        byte[] responseBytes = (byte[]) rawResponse;
        FENetResponse response = responseParser.parse(responseBytes);

        if (!response.isValid()) {
            throw new Exception("Invalid FENet response: " + response.getErrorMessage());
        }

        if (!response.isSuccess()) {
            throw new Exception("FENet error status: 0x" + String.format("%04X", response.getErrorStatus()));
        }

        // Extract data for each tag
        String[] results = new String[tags.size()];
        int dataLengthPerTag = 2; // Default to 2 bytes (Word)

        for (int i = 0; i < tags.size(); i++) {
            tr.com.logidex.cnetdedicated.device.Tag tag = tags.get(i);

            // Determine data length based on tag data type
            switch (tag.getDataType()) {
                case Bit:
                case Byte:
                    dataLengthPerTag = 1;
                    break;
                case Word:
                    dataLengthPerTag = 2;
                    break;
                case Dword:
                    dataLengthPerTag = 4;
                    break;
                case Lword:
                    dataLengthPerTag = 8;
                    break;
            }

            results[i] = response.getBlockDataAsHexString(i, dataLengthPerTag);
        }

        return results;
    }

    public FENetFrameBuilder getFrameBuilder() {
        return frameBuilder;
    }

    public FENetResponseParser getResponseParser() {
        return responseParser;
    }
}
