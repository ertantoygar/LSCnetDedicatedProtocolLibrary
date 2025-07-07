package tr.com.logidex.cnetdedicated.protocol.connection;
import tr.com.logidex.cnetdedicated.app.XGBCNetClient;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
public class TCPConnection implements Connection {
    private Socket socket;
    private TCPConnectionParams params;
    private PrintWriter out;
    private BufferedReader in;
    private TCPReader tcpReader;


    public TCPConnection(TCPConnectionParams tcpConnectionParams) {
        this.params = tcpConnectionParams;
    }


    @Override
    public boolean connect() throws IOException {
        socket = new Socket(params.getIPAddress(), params.getPort());
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        System.out.println("Bağlantı kuruldu!");
        tcpReader = new TCPReader(socket, out, XGBCNetClient.getInstance());
        return true;
    }


    @Override
    public void disconnect() {
        try {
            tcpReader.stopListening();
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Bağlantı kapatılırken hata oluştu: " + e.getMessage());
        }
    }


    @Override
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }


    @Override
    public void sendRequest(String requestMessage, String requestId) throws IOException {
        tcpReader.sendRequest(requestMessage, requestId);
    }


    @Override
    public ResponseReader getResponseReader() {
        return tcpReader;
    }
}
