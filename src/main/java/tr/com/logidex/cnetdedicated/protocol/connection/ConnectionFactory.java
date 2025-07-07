package tr.com.logidex.cnetdedicated.protocol.connection;
public class ConnectionFactory {
    public static Connection createConnection(ConnectionParams params) {
        if (params instanceof SerialConnectionParams) {
            return new SerialConnection((SerialConnectionParams) params);
        } else if (params instanceof TCPConnectionParams) {
            return new TCPConnection((TCPConnectionParams) params);
        } else {
            throw new IllegalArgumentException("Unsopported connection params type: " + params.getClass().getName());
        }
    }
}
