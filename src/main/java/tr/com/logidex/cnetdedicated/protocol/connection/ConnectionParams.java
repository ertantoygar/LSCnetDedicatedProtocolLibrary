package tr.com.logidex.cnetdedicated.protocol.connection;
public abstract class ConnectionParams {

    private  int stationNumber = 2;


    public ConnectionParams(int stationNumber) {
        this.stationNumber = stationNumber;
    }


    public int getStationNumber() {
        return stationNumber;
    }


    @Override
    public abstract String toString();
}
