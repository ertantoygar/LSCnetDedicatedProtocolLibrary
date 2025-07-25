package tr.com.logidex.cnetdedicated.app;
import javafx.beans.property.SimpleBooleanProperty;
import tr.com.logidex.cnetdedicated.device.DataBlock;
import tr.com.logidex.cnetdedicated.device.DataType;
import tr.com.logidex.cnetdedicated.device.RegisteredDataBlock;
import tr.com.logidex.cnetdedicated.device.Tag;
import tr.com.logidex.cnetdedicated.protocol.*;
import tr.com.logidex.cnetdedicated.protocol.connection.*;
import tr.com.logidex.cnetdedicated.protocol.exceptions.FrameCheckException;
import tr.com.logidex.cnetdedicated.protocol.exceptions.NoAcknowledgeMessageFromThePLCException;
import tr.com.logidex.cnetdedicated.protocol.exceptions.NoResponseException;
import tr.com.logidex.cnetdedicated.util.XGBCNetUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
public class XGBCNetClient implements SerialReader.SerialReaderObserver, TCPReader.TCPReaderObserver {
    public static final char ENQ = (char) 0x05;
    public static final char EOT = (char) 0x04;
    private static final boolean USE_FRAME_CHECK = true;
    public static SimpleBooleanProperty touchScreen = new SimpleBooleanProperty(true);
    private static XGBCNetClient instance;
    Logger logger = Logger.getLogger(getClass().getName());
    private int stationNumber;
    private String strStationNumber;
    private Level logLevel = Level.SEVERE;
    private ConcurrentHashMap<String, String> requestResponseMap = new ConcurrentHashMap<>();
    private Map<Integer, List<Tag>> regNumbersAndDevices = new TreeMap<Integer, List<Tag>>();
    /**
     * Represents the delay (in milliseconds) to wait for a response in the XGBCNetClient class.
     */
    private Connection connection;


    private XGBCNetClient() {
        logger.setLevel(logLevel);
    }


    public static XGBCNetClient getInstance() {
        if (instance == null) {
            synchronized (XGBCNetClient.class) {
                instance = new XGBCNetClient();
            }
        }
        return instance;
    }


    public static void setTouchScreen(boolean touchScreen) {
        XGBCNetClient.touchScreen.set(touchScreen);
    }


    public void setLogLevel(Level logLevel) {
        this.logLevel = logLevel;
        logger.setLevel(logLevel);
    }


    public boolean connect(ConnectionParams connectionParams) throws Exception {
        connection = ConnectionFactory.createConnection(connectionParams);
        this.stationNumber = connectionParams.getStationNumber();
        strStationNumber = this.stationNumber < 10 ? "0" + stationNumber : String.valueOf(stationNumber);
        boolean result = connection.connect();
        return result;
    }


    public boolean isConnected() {
        if (connection == null) {
            return false;
        }
        return connection.isConnected();
    }


    public void disconnect() {
        if (isConnected()) {
            connection.disconnect();
        }
    }


    public synchronized void registerDevicesToMonitor(List<Tag> tags, String registrationNumber) throws IOException, NoAcknowledgeMessageFromThePLCException, NoResponseException, FrameCheckException {
        StringBuilder data = new StringBuilder();
        data.append(String.format("%02x", tags.size()));
        // make the data
        for (Tag da : tags) {
            data.append(da.formatToRequest());
        }
        String message = finalizeRequestMessage(Command.X, CommandType.RSS, data.toString(), XGBCNetUtil.addZeroIfNeed(registrationNumber));
        ResponseEvaluator res = sendRequestFrame(message);
        logger.info(res.toString());
        // x ile register isteginin sonucu olumlu ise register olan degiskenleri hafizada tutmaliyiz.
        if (res.getResponse() instanceof AckResponse && res.getResponse().getCommand() == Command.X) {
            String regNo = res.getResponse().getStructrizedDataArea();
            // listemizin regNo indexine device adresleri yazmaliyiz.
            // System.out.println(regNo + " kayit numarasi icin degisken kaydi yapiliyor.. Tag adedi: " + tags.size());
            logger.info(regNo + " kayit numarasi icin degisken kaydi yapiliyor.. Tag adedi: " + tags.size());
            regNumbersAndDevices.put(Integer.parseInt(regNo), tags);
        }
    }


    public synchronized List<Tag> executeRegisteredDeviceToMonitor(String registrationNumber) throws IOException, NoAcknowledgeMessageFromThePLCException, NoResponseException, FrameCheckException {
        registrationNumber = XGBCNetUtil.addZeroIfNeed(registrationNumber);
        String message = finalizeRequestMessage(Command.Y, CommandType.NONE, registrationNumber, registrationNumber);
        ResponseEvaluator re = sendRequestFrame(message);
        logger.info(re.getResponse().toString());
        if (re.getResponse().getCommand() == Command.Y) {
            RegisteredDataBlock rdb = new RegisteredDataBlock();
            String dataAreaForXorY = re.getRawResponse().substring(4, re.getRawResponse().length() - 3);
            //parse registration number
            StringBuilder dataBlockBuilder = new StringBuilder(dataAreaForXorY);
            char[] regNumber = new char[2];
            dataBlockBuilder.getChars(0, 2, regNumber, 0);
            rdb.setRegistrationNumber(regNumber);
            //parse NumberOfBlocks
            char[] numOfBlocks = new char[2];
            dataBlockBuilder.getChars(2, 4, numOfBlocks, 0);
            rdb.setNumberOfBlocks(numOfBlocks);
            //parse all data blocks
            String allDataBlocks = dataBlockBuilder.substring(4, dataAreaForXorY.length());
            rdb.setDataBlocks(parseDataForYCommand(allDataBlocks));
            // update values
            int regNumberInInteger = Integer.parseInt(new String(rdb.getRegistrationNumber()));
            for (Map.Entry<Integer, List<Tag>> entry : regNumbersAndDevices.entrySet()) {
                if (entry.getKey().equals(regNumberInInteger)) {
                    for (int i = 0; i < rdb.getDataBlocks().size(); i++) {
                        String value = rdb.getDataBlocks().get(i).getData();
                        if (!entry.getValue().get(i).dontUpdateProperty().get()) {
                            entry.getValue().get(i).setValueAsHexString(value);
                        }
                    }
                }
            }
            return regNumbersAndDevices.get(regNumberInInteger);
        }
        return null;
    }


    private List<DataBlock> parseDataForYCommand(String input) {
        List<DataBlock> dataBlocks = new ArrayList<>();
        int index = 0;
        while (index < input.length()) {
            // Her bir bloğun başlangıcındaki iki karakteri alarak numberOfData'yı oluştur
            char[] numberOfData = {input.charAt(index), input.charAt(index + 1)};
            index += 2;
            // numberOfData'nın değerini hesapla ve data'yı oluştur
            int numberOfDataValue = Integer.parseInt(new String(numberOfData));
            String data = input.substring(index, index + numberOfDataValue * 2);
            index += numberOfDataValue * 2;
            // Yeni device.cnetdedicated.tr.com.logidex.spreadingmachinecwithcnetprotocol.DataBlock oluştur ve listeye ekle
            DataBlock dataBlock = new DataBlock(numberOfData, data);
            dataBlocks.add(dataBlock);
        }
        return dataBlocks;
    }


    private ResponseEvaluator sendRequestFrame(String requestMessage) throws IOException, NoAcknowledgeMessageFromThePLCException, NoResponseException, FrameCheckException {
        if (!isConnected()) {
            throw new IOException("Mesaj gonderme istegi yapildi, fakat port kapali!");
        }
        logger.log(Level.INFO, "Request       :" + requestMessage);
        String requestId = java.util.UUID.randomUUID().toString(); // Benzersiz bir istek kimliği oluştur
        connection.sendRequest(requestMessage, requestId);
        // Cevabı beklemek için
        long t = System.currentTimeMillis();
        while (connection.getResponseReader().getResponse(requestId) == null) {
            if (System.currentTimeMillis() - t > 3000) {
                throw new NoResponseException();
            }
        }
        String response = connection.getResponseReader().getResponse(requestId);
        logger.log(Level.INFO, "Response for request " + requestId + ": " + response);
        if (response.trim().isEmpty()) {
            throw new NoResponseException();
        }
        if (((char) response.getBytes()[0] != Response.ACK)) {
            throw new NoAcknowledgeMessageFromThePLCException();
        }
        logger.info("The frame is checking...");
        boolean packageIsValid = XGBCNetUtil.checkFrame(response);
        if (packageIsValid) {
            logger.info("Frame check OK!");
        } else {
            logger.severe("Frame check ERROR !!");
            throw new FrameCheckException();
        }
        if (!response.isEmpty()) {
            ResponseEvaluator re = new ResponseEvaluator(response);
            if (re.getResponse() instanceof AckResponse) {
                return re;
            } else {
                logger.log(Level.SEVERE, "ERROR! -> " + re.getResponse());
                System.exit(-1);
            }
        } else {
            logger.log(Level.SEVERE, "No response! Unknown request for the PLC! Probably the BCC value of our request was wrong!");
            System.exit(-1);
        }
        return null;
    }


    /**
     * It prepares a request frame for all types of request messages.
     *
     * @param command            Read or Write
     * @param commandType        Continuous or Individual
     * @param structuredDataArea custom message
     * @param registrationNumber If we don't use command X, there is no need. It can be null.
     * @return Complete message that is ready to send to the PLC.
     */
    private String finalizeRequestMessage(Command command, CommandType commandType, String structuredDataArea, String registrationNumber) {
        StringBuilder requestMessageBuilder = new StringBuilder();
        requestMessageBuilder.append(XGBCNetClient.ENQ);
        requestMessageBuilder.append(strStationNumber);
        requestMessageBuilder.append(USE_FRAME_CHECK ? command.toString().toLowerCase() : command);
        if (command == Command.X) {
            requestMessageBuilder.append(registrationNumber); //Registration number
        }
        if (commandType != CommandType.NONE) {
            requestMessageBuilder.append(commandType.toString().toUpperCase());
        }
        requestMessageBuilder.append(structuredDataArea);
        requestMessageBuilder.append(XGBCNetClient.EOT);
        if (USE_FRAME_CHECK) {
            requestMessageBuilder.append(XGBCNetUtil.calcFrameCheck(requestMessageBuilder.toString()));
        }
        return requestMessageBuilder.toString();
    }


    public synchronized Tag readSingle(Tag tag) throws IOException, NoAcknowledgeMessageFromThePLCException, NoResponseException, FrameCheckException {
        ResponseEvaluator re = sendRequestFrame(finalizeRequestMessage(Command.R, CommandType.SS, "01" + tag.formatToRequest(), null));
        String data = re.getResponse().getStructrizedDataArea();
        tag.setValueAsHexString(data.substring(4));
        return tag;
    }


    public synchronized Tag readSingleString(Tag tag, int countToRead) throws IOException, NoAcknowledgeMessageFromThePLCException, NoResponseException, FrameCheckException {
        if ((tag.getDataType() != DataType.Word)) {
            throw new IllegalArgumentException("The tag's data type must be a word when reading a string!");
        }
        if (countToRead > 64) {
            throw new IllegalArgumentException("The parameter countToRead must not be greater than 64!");
        }
        String charCount = XGBCNetUtil.addZeroIfNeed(String.format("%02x", countToRead / 2));
        ResponseEvaluator re = sendRequestFrame(finalizeRequestMessage(Command.R, CommandType.SB, tag.formatToRequest() + charCount, null));
        String data = re.getResponse().getStructrizedDataArea();
        tag.setValueAsHexString(data.substring(4));
        return tag;
    }


    public synchronized Response writeSingle(Tag tag) throws IOException, NoAcknowledgeMessageFromThePLCException, NoResponseException, FrameCheckException {
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
        String request = finalizeRequestMessage(Command.W, CommandType.SS, data.toString(), null);
        ResponseEvaluator re = sendRequestFrame(request);
        return re.getResponse();
    }


    public synchronized Response writeDouble(Tag tag) throws IOException, NoAcknowledgeMessageFromThePLCException, NoResponseException, FrameCheckException {

        /*
        ENQ
        02
        w
        SB
        06
        %DW220R
        02
        022B __ Decimal 555
        0000
        EOT
        29
        */
        if (tag.getDataType() != DataType.Dword) {
            throw new IllegalArgumentException("This tag type is not a DoubleWord type!");
        }
        String numberOfBlocks = "02";
        String deviceLenAndName = tag.formatToRequest();
        String tagValue = tag.getValueAsHexString();
        StringBuilder data = new StringBuilder();
        String includedDoubleAddr = XGBCNetUtil.multiplyAddress(deviceLenAndName, 2);
        //PLC DD diye bir adres tanımıyor!
        includedDoubleAddr = includedDoubleAddr.replace("DD", "DW");
        data.append(includedDoubleAddr);
        data.append(numberOfBlocks);
        data.append(XGBCNetUtil.swap(tagValue));
        String request = finalizeRequestMessage(Command.W, CommandType.SB, data.toString(), null);
        ResponseEvaluator re = sendRequestFrame(request);
        return re.getResponse();
    }


    public synchronized Response writeSingleString(Tag tag, String strData, int theLimitToWrite) throws IOException, NoAcknowledgeMessageFromThePLCException, NoResponseException, FrameCheckException {
        /*
    Sample write data
    ENQ
    0 2
    w S B
    0 6
    % D W 8 8 6
    0 D
    5 4 5 4 5 4 5 4 5 4 5 4 5 4 5 4 5 4 5 4 5 4 5 4 5 4 5 4 5 4 5 4 5 4 5 4 5 4 5 4 5 4 5 4 5 4 5 4 5 4 5 4
    EOT
    6 1
    */
        if ((tag.getDataType() != DataType.Word)) {
            throw new IllegalArgumentException("The tag's data type must be a word when reading a string!");
        }
        if (theLimitToWrite > 64) {
            throw new IllegalArgumentException("The parameter theLimitToWrite must not be greater than 64!");
        }
        if (strData.length() > theLimitToWrite) { // The given string length is greater than the target.
            strData = strData.substring(0, strData.length() - (strData.length() - theLimitToWrite));
        }
        while (strData.length() < theLimitToWrite) {// The given string length is lower...
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
        String request = finalizeRequestMessage(Command.W, CommandType.SB, data.toString(), null);
        ResponseEvaluator re = sendRequestFrame(request);
        return re.getResponse();
    }


    public synchronized Response writeBit(Tag tag, boolean flag) throws IOException, NoAcknowledgeMessageFromThePLCException, NoResponseException, FrameCheckException {
        if (tag.getDataType() != DataType.Bit) {
            throw new IllegalArgumentException("This tag type is not a Bit type!");
        }
        String value = flag ? "01" : "00";
        String request = finalizeRequestMessage(Command.W, CommandType.SS, "01" + tag.formatToRequest() + value, null);
        ResponseEvaluator re = sendRequestFrame(request);
        if (re.getResponse() instanceof AckResponse) {
            tag.setValueAsHexString(value);
        }
        return re.getResponse();
    }


    public void clearRegisteredDevices() {
        ArrayList<Tag> tags = (ArrayList<Tag>) regNumbersAndDevices.get(9);
        regNumbersAndDevices.clear();
        regNumbersAndDevices.put(9, tags);
    }


    @Override
    public void onDataReceived(String data, String requestId) {
        connection.getResponseReader().setResponse(requestId, data);
    }
}



