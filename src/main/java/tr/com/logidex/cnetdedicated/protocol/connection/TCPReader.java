package tr.com.logidex.cnetdedicated.protocol.connection;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import jdk.jshell.spi.ExecutionControl;
import tr.com.logidex.cnetdedicated.app.XGBCNetClient;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
public class TCPReader implements ResponseReader {
    private final InputStream in;
    private final PrintWriter out;
    public interface TCPReaderObserver {
        void onDataReceived(String data, String requestId);
    }

    private static final Logger logger = Logger.getLogger(TCPReader.class.getName());
    private Socket socket;
    private boolean listening = false;
    private ExecutorService listenerThread;
    private TCPReader.TCPReaderObserver observer;

    private ConcurrentHashMap<String, String> requestResponseMap = new ConcurrentHashMap<>();
    private ConcurrentLinkedQueue<String> requestQueue = new ConcurrentLinkedQueue<>();

    private StringBuilder responseBuffer = new StringBuilder(); // Tampon için StringBuilder


    public TCPReader(Socket socket, PrintWriter out, TCPReader.TCPReaderObserver observer) throws IOException {


        this.socket = socket;
        this.observer = observer;
        this.in = socket.getInputStream();
        this.out=out;




        startListening();



    }


    /**
     * TCP üzerinden gelen verileri dinlemeye başlar.
     */
    private void startListening() {
        if (listening) {
            return;
        }

        listening = true;
        responseBuffer.setLength(0); // Buffer'ı temizle

        listenerThread = Executors.newSingleThreadExecutor();
        listenerThread.submit(() -> {
            byte[] buffer = new byte[1024];
            int bytesRead;

            try {
                while (listening && !socket.isClosed()) {
                    // Veri var mı kontrol et (SerialPort.bytesAvailable() benzeri)
                    if (in.available() > 0) {
                        bytesRead = in.read(buffer);
                        if (bytesRead > 0) {
                            // SerialPort koduna benzer şekilde string oluştur
                            // Ancak String oluşturulurken karakter kodlaması sorunlarını önlemek için
                            // bayt-bayt işleme yapalım
                            StringBuilder responsePartBuilder = new StringBuilder();
                            for (int i = 0; i < bytesRead; i++) {
                                responsePartBuilder.append((char)(buffer[i] & 0xFF));
                            }
                            String responsePart = responsePartBuilder.toString();

                            // Debug amaçlı hex formatında da yazdıralım
                            StringBuilder hexDump = new StringBuilder();
                            for (int i = 0; i < bytesRead; i++) {
                                hexDump.append(String.format("%02X ", buffer[i] & 0xFF));
                            }
                            System.out.println("Alınan ham veri (HEX): " + hexDump.toString());

                            // Seri port kodunuzla aynı mantıkta veri işleme
                            responseBuffer.append(responsePart);
                            System.out.println("Yanıt buffer: " + responseBuffer.toString());

                            // XGT protokolüne özgü mesaj ayırma işlemi yapabilirsiniz
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

        System.out.println("Gönderiliyor (" + requestId + "): " + request);
        out.println(request);


        requestQueue.add(requestId);

    }


    @Override
    public ConcurrentHashMap<String, String> getRequestResponseMap() {
       return null;
    }


    public String getResponse(String requestId) {
        return requestResponseMap.get(requestId);
    }

    public void setResponse(String requestId, String response) {
        requestResponseMap.put(requestId, response);
    }
}

