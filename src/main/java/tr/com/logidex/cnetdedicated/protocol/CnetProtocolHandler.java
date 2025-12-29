package tr.com.logidex.cnetdedicated.protocol;

import tr.com.logidex.cnetdedicated.device.DataType;
import tr.com.logidex.cnetdedicated.device.Tag;
import tr.com.logidex.cnetdedicated.util.XGBCNetUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Cnet Protocol Handler for Serial (RS232/RS485) communication
 * Uses ASCII-based framing with ENQ/EOT control characters
 */
public class CnetProtocolHandler implements ProtocolHandler {

    private static final char ENQ = (char) 0x05;
    private static final char EOT = (char) 0x04;
    private static final boolean USE_FRAME_CHECK = true;

    private final String stationNumber;

    public CnetProtocolHandler(int stationNumber) {
        this.stationNumber = stationNumber < 10 ? "0" + stationNumber : String.valueOf(stationNumber);
    }

    @Override
    public Object buildReadRequest(Tag tag, CommandType commandType) throws IOException {
        return finalizeRequestMessage(Command.R, commandType, "01" + tag.formatToRequest(), null);
    }

    @Override
    public Object buildWriteRequest(Tag tag, CommandType commandType) throws IOException {
        if (tag.getDataType() != DataType.Word) {
            throw new IllegalArgumentException("This tag type is not a Word type!");
        }
        String numberOfBlocks = "01";
        String deviceLenAndName = tag.formatToRequest();
        String tagValue = tag.getValueAsHexString();
        StringBuilder data = new StringBuilder();
        data.append(numberOfBlocks);
        data.append(deviceLenAndName);
        data.append(tagValue);
        return finalizeRequestMessage(Command.W, commandType, data.toString(), null);
    }

    @Override
    public Object buildWriteDoubleRequest(Tag tag) throws IOException {
        if (tag.getDataType() != DataType.Dword) {
            throw new IllegalArgumentException("This tag type is not a DoubleWord type!");
        }
        String numberOfBlocks = "02";
        String deviceLenAndName = tag.formatToRequest();
        String tagValue = tag.getValueAsHexString();
        StringBuilder data = new StringBuilder();
        String includedDoubleAddr = XGBCNetUtil.multiplyAddress(deviceLenAndName, 2);
        includedDoubleAddr = includedDoubleAddr.replace("DD", "DW");
        data.append(includedDoubleAddr);
        data.append(numberOfBlocks);
        data.append(XGBCNetUtil.swap(tagValue));
        return finalizeRequestMessage(Command.W, CommandType.SB, data.toString(), null);
    }

    @Override
    public Object buildWriteBitRequest(Tag tag, boolean value) throws IOException {
        if (tag.getDataType() != DataType.Bit) {
            throw new IllegalArgumentException("This tag type is not a Bit type!");
        }
        String valueStr = value ? "01" : "00";
        return finalizeRequestMessage(Command.W, CommandType.SS, "01" + tag.formatToRequest() + valueStr, null);
    }

    @Override
    public Object buildWriteStringRequest(Tag tag, String strData, int theLimitToWrite) throws IOException {
        if ((tag.getDataType() != DataType.Word)) {
            throw new IllegalArgumentException("The tag's data type must be a word when reading a string!");
        }
        if (theLimitToWrite > 64) {
            throw new IllegalArgumentException("The parameter theLimitToWrite must not be greater than 64!");
        }
        if (strData.length() > theLimitToWrite) {
            strData = strData.substring(0, strData.length() - (strData.length() - theLimitToWrite));
        }
        while (strData.length() < theLimitToWrite) {
            strData += " ";
        }
        StringBuilder data = new StringBuilder();
        String deviceLenAndName = tag.formatToRequest();
        data.append(deviceLenAndName);
        int stringLen = strData.length();
        if (stringLen % 2 != 0) {
            strData = strData + " ";
            stringLen++;
        }
        int _len = stringLen;
        if (tag.getDataType() == DataType.Word)
            _len /= 2;
        String lenInHex = Integer.toHexString(_len);
        data.append(XGBCNetUtil.addZeroIfNeed(lenInHex));
        strData = XGBCNetUtil.convertTurkishToEnglish(strData);
        byte[] bytes = Arrays.copyOf(strData.getBytes(StandardCharsets.US_ASCII), stringLen);
        for (byte b : bytes) {
            String s = XGBCNetUtil.addZeroIfNeed(Integer.toHexString(b));
            data.append(s);
        }
        return finalizeRequestMessage(Command.W, CommandType.SB, data.toString(), null);
    }

    @Override
    public Object buildRegisterRequest(List<Tag> tags, String registrationNumber) throws IOException {
        StringBuilder data = new StringBuilder();
        data.append(String.format("%02x", tags.size()));
        for (Tag da : tags) {
            data.append(da.formatToRequest());
        }
        return finalizeRequestMessage(Command.X, CommandType.RSS, data.toString(), XGBCNetUtil.addZeroIfNeed(registrationNumber));
    }

    @Override
    public Object buildExecuteRequest(String registrationNumber) throws IOException {
        registrationNumber = XGBCNetUtil.addZeroIfNeed(registrationNumber);
        return finalizeRequestMessage(Command.Y, CommandType.NONE, registrationNumber, registrationNumber);
    }

    @Override
    public String parseResponse(Object rawResponse) throws Exception {
        if (!(rawResponse instanceof String)) {
            throw new IllegalArgumentException("Cnet expects String response");
        }
        String response = (String) rawResponse;
        ResponseEvaluator re = new ResponseEvaluator(response);
        if (re.getResponse() instanceof AckResponse) {
            return re.getResponse().getStructrizedDataArea();
        }
        throw new Exception("Invalid response: " + re.getResponse());
    }

    @Override
    public boolean isResponseValid(Object rawResponse) {
        if (!(rawResponse instanceof String)) {
            return false;
        }
        String response = (String) rawResponse;
        if (response.isEmpty()) {
            return false;
        }
        return response.charAt(0) == Response.ACK;
    }

    @Override
    public Command getCommandFromResponse(Object rawResponse) {
        if (!(rawResponse instanceof String)) {
            return null;
        }
        String response = (String) rawResponse;
        ResponseEvaluator re = new ResponseEvaluator(response);
        return re.getResponse().getCommand();
    }

    @Override
    public String getStructuredDataArea(Object rawResponse) {
        if (!(rawResponse instanceof String)) {
            return "";
        }
        String response = (String) rawResponse;
        ResponseEvaluator re = new ResponseEvaluator(response);
        return re.getResponse().getStructrizedDataArea();
    }

    @Override
    public boolean isBinaryProtocol() {
        return false;
    }

    /**
     * Finalize Cnet request message with ENQ/EOT framing
     */
    private String finalizeRequestMessage(Command command, CommandType commandType, String structuredDataArea, String registrationNumber) {
        StringBuilder requestMessageBuilder = new StringBuilder();
        requestMessageBuilder.append(ENQ);
        requestMessageBuilder.append(stationNumber);
        requestMessageBuilder.append(USE_FRAME_CHECK ? command.toString().toLowerCase() : command);
        if (command == Command.X) {
            requestMessageBuilder.append(registrationNumber);
        }
        if (commandType != CommandType.NONE) {
            requestMessageBuilder.append(commandType.toString().toUpperCase());
        }
        requestMessageBuilder.append(structuredDataArea);
        requestMessageBuilder.append(EOT);
        if (USE_FRAME_CHECK) {
            requestMessageBuilder.append(XGBCNetUtil.calcFrameCheck(requestMessageBuilder.toString()));
        }
        return requestMessageBuilder.toString();
    }
}
