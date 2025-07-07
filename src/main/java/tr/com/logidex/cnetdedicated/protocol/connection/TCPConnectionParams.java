package tr.com.logidex.cnetdedicated.protocol.connection;
public class TCPConnectionParams extends ConnectionParams {
    private String IPAddress;
    private int Port;


    public TCPConnectionParams(String ipAddress, int port, int stationNumber) {
        super(stationNumber);
        this.IPAddress = ipAddress;
        this.Port = port;
    }


    @Override
    public int getStationNumber() {
        return super.getStationNumber();
    }


    public int getPort() {
        return Port;
    }


    public String getIPAddress() {
        return IPAddress;
    }


    @Override
    public String toString() {
        return "";
    }
}
