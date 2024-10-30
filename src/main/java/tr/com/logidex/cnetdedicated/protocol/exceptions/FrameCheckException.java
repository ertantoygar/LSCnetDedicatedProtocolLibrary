package tr.com.logidex.cnetdedicated.protocol.exceptions;
public class FrameCheckException extends Exception {
    public FrameCheckException() {
        super("Plc'den gelen mesajda veri kaybı tespit edildi!");
    }
}
