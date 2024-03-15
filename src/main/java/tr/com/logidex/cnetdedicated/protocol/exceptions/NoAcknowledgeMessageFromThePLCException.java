package tr.com.logidex.cnetdedicated.protocol.exceptions;

public class NoAcknowledgeMessageFromThePLCException extends Throwable {
    public NoAcknowledgeMessageFromThePLCException() {

        super("We could not get an ACK message for the message we sent!");
    }
}
