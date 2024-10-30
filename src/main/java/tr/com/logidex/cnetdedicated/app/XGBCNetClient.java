package tr.com.logidex.cnetdedicated.app;
import com.fazecast.jSerialComm.SerialPort;
import javafx.beans.property.SimpleBooleanProperty;
import tr.com.logidex.cnetdedicated.device.DataBlock;
import tr.com.logidex.cnetdedicated.device.DataType;
import tr.com.logidex.cnetdedicated.device.RegisteredDataBlock;
import tr.com.logidex.cnetdedicated.device.Tag;
import tr.com.logidex.cnetdedicated.protocol.*;
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

/**
 * The XGBCNetClient class is responsible for managing the communication with
 * the PLC (Programmable Logic Controller) with the cnet protocol using Serial Port communication.
 * This class is a singleton and ensures that only one instance is used
 * throughout the application.
 */
public class XGBCNetClient implements SerialReader.SerialReaderObserver {
    public static final char ENQ = (char) 0x05;
    public static final char EOT = (char) 0x04;
    private static final boolean USE_FRAME_CHECK = true;
    public static SimpleBooleanProperty touchScreen = new SimpleBooleanProperty(true);
    private static XGBCNetClient instance;
    Logger logger = Logger.getLogger(getClass().getName());
    private SerialPort serialPort;
    private int stationNumber;
    private String strStationNumber;
    private Level logLevel = Level.SEVERE;
    private ConcurrentHashMap<String, String> requestResponseMap = new ConcurrentHashMap<>();
    private Map<Integer, List<Tag>> regNumbersAndDevices = new TreeMap<Integer, List<Tag>>();
    /**
     * Represents the delay (in milliseconds) to wait for a response in the XGBCNetClient class.
     */
    private SerialReader serialReader;


    private XGBCNetClient() {
        logger.setLevel(logLevel);
    }


    /**
     * Provides a global point of access to the XGBCNetClient singleton instance.
     *
     * @return The singleton instance of XGBCNetClient.
     */
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


    /**
     * Converts the given parity enumeration value to its corresponding integer value
     * as defined by the SerialPort constants.
     *
     * @param parity The parity enumeration value to be converted.
     *               It could be one of Parity.odd, Parity.even, Parity.none, Parity.mark, or Parity.space.
     * @return An integer representing the parity value in SerialPort standards.
     * Returns SerialPort.ODD_PARITY for Parity.odd,
     * SerialPort.EVEN_PARITY for Parity.even,
     * SerialPort.NO_PARITY for Parity.none,
     * SerialPort.MARK_PARITY for Parity.mark,
     * and SerialPort.SPACE_PARITY for Parity.space.
     * Defaults to 0 if no valid parity is provided.
     */
    private int parityValueOf(Parity parity) {
        int intParity = 0;
        switch (parity) {
            case odd:
                intParity = SerialPort.ODD_PARITY;
                break;
            case even:
                intParity = SerialPort.EVEN_PARITY;
                break;
            case none:
                intParity = SerialPort.NO_PARITY;
                break;
            case mark:
                intParity = SerialPort.MARK_PARITY;
                break;
            case space:
                intParity = SerialPort.SPACE_PARITY;
                break;
            default:
                break;
        }
        return intParity;
    }


    /**
     * Sets the baud rate for the serial port communication.
     *
     * @param newBaudRate the new baud rate to set for the serial port.
     */
    public void setBaudRate(int newBaudRate) {
        serialPort.setBaudRate(newBaudRate);
    }


    /**
     * Sets the parity mode for the serial port.
     *
     * @param parity The parity mode to set. It can be one of the following values from the Parity enumeration:
     *               - Parity.odd
     *               - Parity.even
     *               - Parity.none
     *               - Parity.mark
     *               - Parity.space
     */
    public void setParity(Parity parity) {
        serialPort.setParity(parityValueOf(parity));
    }


    /**
     * Sets the number of data bits for the serial port communication.
     *
     * @param newNumDataBits the number of data bits to set for the serial port.
     */
    public void setNumDataBits(int newNumDataBits) {
        serialPort.setNumDataBits(newNumDataBits);
    }


    /**
     * Sets the number of stop bits for the serial port communication.
     *
     * @param newNumStopBits The number of stop bits to set for the serial port.
     *                       Valid values are typically 1 or 2, depending on the serial port's capabilities.
     */
    public void setNumStopBits(int newNumStopBits) {
        serialPort.setNumStopBits(newNumStopBits);
    }


    /**
     * Sets the logging level for this client and updates the logger's level accordingly.
     *
     * @param logLevel the desired logging level to be set. This could be one of the predefined
     *                 levels in the {@code Level} class, such as {@code Level.INFO}, {@code Level.WARNING},
     *                 {@code Level.SEVERE}, etc.
     */
    public void setLogLevel(Level logLevel) {
        this.logLevel = logLevel;
        logger.setLevel(logLevel);
    }


    /**
     * Establishes a connection to a serial port using the specified connection parameters.
     *
     * @param connectionParams The parameters required to establish the connection. This includes port name,
     *                         baud rate, parity, number of data bits, number of stop bits, and
     *                         station number.
     * @return true if the connection was successfully established, false otherwise.
     */
    public boolean connect(ConnectionParams connectionParams) {
        serialPort = SerialPort.getCommPort(connectionParams.getPortName());
        serialPort.setBaudRate(connectionParams.getBaudRate());
        serialPort.setParity(parityValueOf(connectionParams.getParity()));
        serialPort.setNumDataBits(connectionParams.getDataBits());
        serialPort.setNumStopBits(connectionParams.getStopBits());
        this.stationNumber = connectionParams.getStationNumber();
        strStationNumber = this.stationNumber < 10 ? "0" + stationNumber : String.valueOf(stationNumber);
        boolean result = serialPort.openPort();
        serialReader = new SerialReader(serialPort, this);
        return result;
    }


    /**
     * Checks if the serial port is currently open.
     *
     * @return true if the serial port is open, false otherwise.
     */
    public boolean isPortOpen() {
        if (serialPort == null) {
            return false;
        }
        return serialPort.isOpen();
    }


    /**
     * Closes the serial port if it is currently open.
     * This method checks the state of the serial port and invokes its close operation.
     * No action is taken if the port is already closed.
     */
    public void closePort() {
        if (serialPort.isOpen()) {
            serialPort.closePort();
        }
    }


    /**
     * Registers a list of devices to be monitored using a specified registration number.
     * This method sends a request frame to the PLC and processes the acknowledgment.
     *
     * @param tags               A list of Tag objects representing the devices to be monitored.
     * @param registrationNumber A String containing the registration number used for the request.
     * @throws IOException                             If there is an I/O error during the communication.
     * @throws NoAcknowledgeMessageFromThePLCException If no acknowledgment message is received from the PLC.
     * @throws NoResponseException                     If no response is received from the PLC.
     * @throws FrameCheckException                     If there is a frame check error.
     */
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


    /**
     * Executes the process of monitoring a registered device by its registration number.
     * It sends a request frame to the PLC and processes the response to update device values.
     *
     * @param registrationNumber The registration number of the device to be monitored.
     *                           This number uniquely identifies the device.
     * @return A list of Tag objects representing the device's status and properties
     * if the monitoring is successful; null otherwise.
     * @throws IOException                             If there is an I/O error during the communication.
     * @throws NoAcknowledgeMessageFromThePLCException If no acknowledgment message is received from the PLC.
     * @throws NoResponseException                     If no response is received from the PLC.
     * @throws FrameCheckException                     If there is a frame check error.
     */
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


    /**
     * Parses the input string to extract data blocks for the Y command.
     *
     * @param input the input string containing the data to be parsed.
     * @return a list of DataBlock objects extracted from the input string.
     */
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


    /**
     * Sends a request frame to the PLC and waits for a response.
     * This method manages the communication with the PLC by sending a request message,
     * waiting for a response, and validating the received frame.
     *
     * @param requestMessage The message to be sent to the PLC.
     * @return A ResponseEvaluator object containing the response received from the PLC.
     * @throws IOException                             If the port is not open.
     * @throws NoAcknowledgeMessageFromThePLCException If the PLC does not acknowledge the message.
     * @throws NoResponseException                     If no response is received from the PLC within the specified timeout.
     * @throws FrameCheckException                     If the received frame is invalid.
     */
    private ResponseEvaluator sendRequestFrame(String requestMessage) throws IOException, NoAcknowledgeMessageFromThePLCException, NoResponseException, FrameCheckException {
        if (!isPortOpen()) {
            throw new IOException("A request was tried, but the port is not open");
        }
        logger.log(Level.INFO, "Request       :" + requestMessage);
        String requestId = java.util.UUID.randomUUID().toString(); // Benzersiz bir istek kimliği oluştur
        serialReader.sendRequest(requestMessage, requestId);
        // Cevabı beklemek için
        long t = System.currentTimeMillis();
        while (serialReader.getResponse(requestId) == null) {
            if (System.currentTimeMillis() - t > 3000) {
                throw new NoResponseException();
            }
        }
        String response = serialReader.getResponse(requestId);
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


    /**
     * Reads the value of a single tag from the PLC.
     *
     * @param tag The tag to be read. It contains information such as the address and data type.
     * @return The tag with its value updated from the PLC.
     * @throws IOException                             If an I/O error occurs during communication.
     * @throws NoAcknowledgeMessageFromThePLCException If no acknowledgment message is received from the PLC.
     * @throws NoResponseException                     If no response is received from the PLC.
     * @throws FrameCheckException                     If there is a frame check error.
     */
    public synchronized Tag readSingle(Tag tag) throws IOException, NoAcknowledgeMessageFromThePLCException, NoResponseException, FrameCheckException {
        ResponseEvaluator re = sendRequestFrame(finalizeRequestMessage(Command.R, CommandType.SS, "01" + tag.formatToRequest(), null));
        String data = re.getResponse().getStructrizedDataArea();
        tag.setValueAsHexString(data.substring(4));
        return tag;
    }


    /**
     * Reads a single string from the given tag with the specified number of characters.
     *
     * @param tag         the tag from which the string is to be read; the data type must be Word.
     * @param countToRead the number of characters to read; must not be greater than 64.
     * @return the updated tag containing the read string value.
     * @throws IOException                             if an I/O error occurs during communication.
     * @throws NoAcknowledgeMessageFromThePLCException if there is no acknowledgment message from the PLC.
     * @throws NoResponseException                     if no response is received from the PLC.
     * @throws FrameCheckException                     if a frame check error occurs.
     * @throws IllegalArgumentException                if the tag's data type is not Word or if countToRead is greater than 64.
     */
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


    /**
     * Writes a single Word type value to a specified Tag in the PLC.
     *
     * @param tag the Tag to which the Word value will be written. The tag must be of DataType.Word.
     * @return the Response received after the write operation.
     * @throws IllegalArgumentException                if the tag is not of DataType.Word.
     * @throws IOException                             if an I/O error occurs during communication.
     * @throws NoAcknowledgeMessageFromThePLCException if no acknowledge message is received from the PLC.
     * @throws NoResponseException                     if no response is received from the PLC.
     * @throws FrameCheckException                     if there is a frame check error in the communication.
     */
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


    /**
     * Writes a double word value to a specified tag on the PLC.
     *
     * @param tag the tag to be written, which must be of type DoubleWord
     * @return the response from the PLC after attempting to write the value
     * @throws IOException                             if an I/O error occurs while communicating with the PLC
     * @throws NoAcknowledgeMessageFromThePLCException if no acknowledgment is received from the PLC
     * @throws NoResponseException                     if no response is received from the PLC
     * @throws FrameCheckException                     if there is a frame check error while sending the request
     */
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


    /**
     * Writes a single string to a specified PLC tag, ensuring the string adheres to
     * the length constraints and the data type requirements for the target tag.
     *
     * @param tag             The tag representing the data point in the PLC where the string will be written.
     *                        The tag must have a data type of 'Word'.
     * @param strData         The string data to be written to the specified tag.
     * @param theLimitToWrite The maximum length of the string that can be written.
     *                        Must not exceed 64 characters.
     * @return A Response object containing the result of the write operation.
     * @throws IOException                             If an I/O error occurs during the communication with the PLC.
     * @throws NoAcknowledgeMessageFromThePLCException If no acknowledgment is received from the PLC.
     * @throws NoResponseException                     If no response is received from the PLC.
     * @throws FrameCheckException                     If there is a frame check error.
     */
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


    /**
     * Writes a bit value to a specified tag on the PLC.
     *
     * @param tag  The tag representing the bit value to write. The tag's data type must be Bit.
     * @param flag The boolean value to write to the tag. If true, the bit is set to 1; otherwise, it is set to 0.
     * @return The response received from the PLC after attempting to write the bit value.
     * @throws IOException                             If an I/O error occurs during communication with the PLC.
     * @throws NoAcknowledgeMessageFromThePLCException If the PLC does not acknowledge the write request.
     * @throws NoResponseException                     If no response is received from the PLC within the expected time frame.
     * @throws FrameCheckException                     If the frame check sequence indicates an error.
     */
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


    /**
     * Clears all registered devices in the system while retaining the tags
     * associated with the key 9. This method first retrieves the list of tags
     * mapped to the key 9, clears the entire map of registered devices, and then
     * reinserts the previously retrieved list of tags back into the map under
     * the key 9.
     */
    public void clearRegisteredDevices() {
        ArrayList<Tag> tags = (ArrayList<Tag>) regNumbersAndDevices.get(9);
        regNumbersAndDevices.clear();
        regNumbersAndDevices.put(9, tags);
    }


    /**
     * This method is called when data is received from the serial port.
     *
     * @param data      The data received as a String.
     * @param requestId The unique identifier for the request associated with the received data.
     */
    @Override
    public void onDataReceived(String data, String requestId) {
        serialReader.setResponse(requestId, data);
    }
}



