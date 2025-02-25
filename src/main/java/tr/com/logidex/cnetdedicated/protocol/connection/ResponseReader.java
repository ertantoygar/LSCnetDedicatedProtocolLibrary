package tr.com.logidex.cnetdedicated.protocol.connection;
import java.io.IOException;
public interface ResponseReader {
    String getResponse(String requestId);
    void setResponse(String requestId, String data);
    void sendRequest(String request, String requestId) throws IOException;
}
