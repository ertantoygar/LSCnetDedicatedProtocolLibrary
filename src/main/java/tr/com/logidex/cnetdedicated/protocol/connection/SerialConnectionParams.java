package tr.com.logidex.cnetdedicated.protocol.connection;
import tr.com.logidex.cnetdedicated.protocol.Parity;
public class SerialConnectionParams extends ConnectionParams {


    private String portName;
    private int baudRate;
    private Parity parity = Parity.none;
    private int dataBits = 8;
    private int stopBits = 1;

    public SerialConnectionParams(String portName, int baudRate, Parity parity, int dataBits, int
                                 stopBits, int stationNumber) {
        super(stationNumber);
        this.portName=portName;
        this.baudRate=baudRate;
        this.parity=parity;
        this.dataBits=dataBits;
        this.stopBits=stopBits;


    }

    public String getPortName() {
        return portName;
    }

    public int getBaudRate() {
        return baudRate;
    }

    public Parity getParity() {
        return parity;
    }

    public int getDataBits() {
        return dataBits;
    }

    public int getStopBits() {
        return stopBits;
    }




    @Override
    public String toString() {
        return  " Port: "+portName + ", " + " Baudrate: " +baudRate + ", " + " Parity: " + parity + ", " +" Databits: "+ dataBits+ ", " + " Stopbits: "+stopBits + ", " + " Station number: "+ getStationNumber();
    }
}
