package tr.com.logidex.cnetdedicated.protocol.connection;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
public class SerialReader implements ResponseReader {

    public interface SerialReaderObserver {
        void onDataReceived(String data, String requestId);
    }

    private static final Logger logger = Logger.getLogger(SerialReader.class.getName());
    private SerialPort serialPort;
    private SerialReaderObserver observer;

    private ConcurrentHashMap<String, String> requestResponseMap = new ConcurrentHashMap<>();
    private ConcurrentLinkedQueue<String> requestQueue = new ConcurrentLinkedQueue<>();

    private StringBuilder responseBuffer = new StringBuilder(); // Tampon için StringBuilder


    public SerialReader(SerialPort serialPort, SerialReaderObserver observer) {
        this.serialPort = serialPort;
        this.observer = observer;
        this.serialPort.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }

            @Override
            public void serialEvent(SerialPortEvent event) { if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
                return;
            }
                byte[] buffer = new byte[serialPort.bytesAvailable()];
                int numRead = serialPort.readBytes(buffer, buffer.length);
                String responsePart = new String(buffer);
                responseBuffer.append(responsePart); // Gelen parçayı tampona ekle

                // Tam mesajın sonunu belirlemek için ETX (0x03) karakterini ve ardından iki hex karakteri kontrol et
                String currentBuffer = responseBuffer.toString();
                int etxIndex = currentBuffer.indexOf((char) 0x03);
                if (etxIndex != -1 && currentBuffer.length() >= etxIndex + 3) {
                    String completeResponse = currentBuffer.substring(0, etxIndex + 3);
                    responseBuffer.delete(0, etxIndex + 3); // Tamponu temizle
                    String requestId = requestQueue.poll(); // Cevap için sıradaki istek kimliğini al
                    if (observer != null && requestId != null) {
                        observer.onDataReceived(completeResponse, requestId);
                    }
                }
            }
        });
    }

    public void sendRequest(String request, String requestId) throws IOException {
        requestQueue.add(requestId);
        int wrote = serialPort.writeBytes(request.getBytes(), request.length());
        if (wrote == -1) {
            throw new IOException("Error while sending request frame!");
        }

    }

    public String getResponse(String requestId) {
        return requestResponseMap.get(requestId);
    }

    public void setResponse(String requestId, String response) {
        requestResponseMap.put(requestId, response);
    }
}

