package tr.com.logidex.cnetdedicated.protocol.connection;
import java.io.IOException;
public interface Connection {
    boolean connect() throws Exception;
    void disconnect();
    boolean isConnected();
    void sendRequest(String requestMessage, String requestId) throws IOException;
    void sendRequest(byte[] requestMessage, String requestId) throws IOException;
    ResponseReader getResponseReader();
    boolean isBinaryProtocol();
}
