package tr.com.logidex.cnetdedicated.protocol.connection;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
public interface ResponseReader {
    String getResponse(String requestId);
    void setResponse(String requestId, String data);
    void sendRequest(String request, String requestId) throws IOException;
    ConcurrentHashMap<String, String> getRequestResponseMap();


      default void clearResponse(String requestId) {
        if (getRequestResponseMap().containsKey(requestId)) {
            getRequestResponseMap().remove(requestId);
        }
    }
}
