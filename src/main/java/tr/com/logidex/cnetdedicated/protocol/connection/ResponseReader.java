package tr.com.logidex.cnetdedicated.protocol.connection;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
public interface ResponseReader {
    Object getResponse(String requestId);
    void setResponse(String requestId, Object data);
    void sendRequest(String request, String requestId) throws IOException;
    ConcurrentHashMap<String, ?> getRequestResponseMap();
    default void clearResponse(String requestId) {
        if (getRequestResponseMap().containsKey(requestId)) {
            getRequestResponseMap().remove(requestId);
        }
    }
}
