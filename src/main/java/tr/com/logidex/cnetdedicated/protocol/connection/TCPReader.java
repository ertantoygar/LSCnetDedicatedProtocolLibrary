package tr.com.logidex.cnetdedicated.protocol.connection;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
public class TCPReader implements ResponseReader {
    private static final Logger logger = Logger.getLogger(TCPReader.class.getName());
    private final InputStream in;
    private final OutputStream out;
    private Socket socket;
    private boolean listening = false;
    private ExecutorService listenerThread;
    private TCPReader.TCPReaderObserver observer;
    private ConcurrentHashMap<String, Object> requestResponseMap = new ConcurrentHashMap<>();
    private ConcurrentLinkedQueue<String> requestQueue = new ConcurrentLinkedQueue<>();
    private ByteArrayOutputStream binaryResponseBuffer = new ByteArrayOutputStream(); // For binary (FENet) responses
    private static final int FENET_HEADER_LENGTH = 20;

    public TCPReader(Socket socket, PrintWriter printWriter, TCPReader.TCPReaderObserver observer) throws IOException {
        this.socket = socket;
        this.observer = observer;
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
        startListening();
    }


    /**
     * TCP üzerinden gelen verileri dinlemeye başlar.
     * FENet binary protocol için optimize edilmiştir.
     */
    private void startListening() {
        if (listening) {
            return;
        }
        listening = true;
        binaryResponseBuffer.reset(); // Buffer'ı temizle
        listenerThread = Executors.newSingleThreadExecutor();
        listenerThread.submit(() -> {
            byte[] buffer = new byte[1024];
            int bytesRead;
            try {
                while (listening && !socket.isClosed()) {
                    // Veri var mı kontrol et
                    if (in.available() > 0) {
                        bytesRead = in.read(buffer);
                        if (bytesRead > 0) {
                            // Alınan verileri binary buffer'a ekle
                            binaryResponseBuffer.write(buffer, 0, bytesRead);

                            // Debug amaçlı hex formatında yazdır
                            StringBuilder hexDump = new StringBuilder();
                            for (int i = 0; i < bytesRead; i++) {
                                hexDump.append(String.format("%02X ", buffer[i] & 0xFF));
                            }
                            System.out.println("Alınan ham veri (HEX): " + hexDump.toString());

                            // FENet frame'inin tamamını kontrol et
                            if (isFENetFrameComplete(binaryResponseBuffer.toByteArray())) {
                                byte[] completeResponse = binaryResponseBuffer.toByteArray();
                                System.out.println("Tam FENet frame alındı: " + completeResponse.length + " bytes");

                                binaryResponseBuffer.reset(); // Tamponu temizle
                                String requestId = requestQueue.poll(); // Cevap için sıradaki istek kimliğini al

                                if (observer != null && requestId != null) {
                                    observer.onDataReceived(completeResponse, requestId);
                                }
                            }
                        }
                    }
                    // CPU kullanımını azaltmak için kısa bir bekleme
                    Thread.sleep(10);
                }
            } catch (IOException | InterruptedException e) {
                if (listening) {
                    System.err.println("Veri dinleme hatası: " + e.getMessage());
                }
            }
        });
    }

    /**
     * FENet frame'inin tam olup olmadığını kontrol eder
     */
    private boolean isFENetFrameComplete(byte[] data) {
        if (data.length < FENET_HEADER_LENGTH) {
            return false;
        }

        // Payload uzunluğunu oku (bytes 16-17, little-endian)
        int payloadLength = ((data[17] & 0xFF) << 8) | (data[16] & 0xFF);

        // Toplam frame uzunluğu = 20 byte header + payload length
        int expectedLength = FENET_HEADER_LENGTH + payloadLength;

        System.out.println("Payload length: " + payloadLength + ", Expected total: " + expectedLength + ", Actual: " + data.length);

        return data.length >= expectedLength;
    }


    public void stopListening() {
        listening = false;
        if (listenerThread != null) {
            listenerThread.shutdownNow();
            listenerThread = null;
        }
    }


    public void sendRequest(String request, String requestId) throws IOException {
        if (!socket.isConnected()) {
            throw new IOException("Bağlantı kapalı. Önce connect() metodu ile bağlantı kurun.");
        }
        System.out.println("Gönderiliyor String (" + requestId + "): " + request);
        out.write(request.getBytes());
        out.flush();
        requestQueue.add(requestId);
    }

    public void sendRequest(byte[] request, String requestId) throws IOException {
        if (!socket.isConnected()) {
            throw new IOException("Bağlantı kapalı. Önce connect() metodu ile bağlantı kurun.");
        }
        // Debug: Print hex dump
        StringBuilder hexDump = new StringBuilder();
        for (byte b : request) {
            hexDump.append(String.format("%02X ", b & 0xFF));
        }
        System.out.println("Gönderiliyor Binary (" + requestId + "): " + hexDump.toString());
        System.out.println("Binary length: " + request.length + " bytes");

        out.write(request);
        out.flush();
        requestQueue.add(requestId);
    }


    @Override
    public ConcurrentHashMap<String, Object> getRequestResponseMap() {
        return requestResponseMap;
    }


    public Object getResponse(String requestId) {
        return requestResponseMap.get(requestId);
    }


    public void setResponse(String requestId, Object response) {
        requestResponseMap.put(requestId, response);
    }


    public interface TCPReaderObserver {
        void onDataReceived(Object data, String requestId);
    }
}

