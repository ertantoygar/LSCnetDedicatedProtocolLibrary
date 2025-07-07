package tr.com.logidex.cnetdedicated.protocol.connection;
import com.fazecast.jSerialComm.SerialPort;
import tr.com.logidex.cnetdedicated.app.XGBCNetClient;
import tr.com.logidex.cnetdedicated.protocol.Parity;

import java.io.IOException;
public class SerialConnection implements Connection {




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



    private SerialPort serialPort;
    private SerialReader serialReader;

    public SerialConnection(SerialConnectionParams params) {

        serialPort = SerialPort.getCommPort(params.getPortName());
        serialPort.setBaudRate(params.getBaudRate());
        serialPort.setParity(parityValueOf(params.getParity()));
        serialPort.setNumDataBits(params.getDataBits());
        serialPort.setNumStopBits(params.getStopBits());

        serialReader = new SerialReader(serialPort, XGBCNetClient.getInstance());


    }


    @Override
    public boolean connect() {
        return serialPort.openPort();
    }


    @Override
    public void disconnect() {
        serialPort.closePort();
    }


    @Override
    public boolean isConnected() {
      return  serialPort.isOpen();

    }


    @Override
    public void sendRequest(String requestMessage, String requestId) throws IOException {
        serialReader.sendRequest(requestMessage,requestId);
    }


    @Override
    public ResponseReader getResponseReader() {
        return serialReader;
    }



}
