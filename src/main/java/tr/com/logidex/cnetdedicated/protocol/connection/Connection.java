package tr.com.logidex.cnetdedicated.protocol.connection;
import java.io.IOException;
public interface Connection {

    boolean connect();
    void disconnect();
    boolean isConnected();
    void sendRequest(String requestMessage, String requestId) throws IOException;
    ResponseReader getResponseReader();
}
