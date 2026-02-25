package tr.com.logidex.cnetdedicated.protocol;

import tr.com.logidex.cnetdedicated.device.Tag;

import java.io.IOException;
import java.util.List;

/**
 * Protocol Handler interface for PLC communication.
 * Supports both Cnet (Serial) and FENet (TCP) protocols.
 */
public interface ProtocolHandler {

    /**
     * Build a read request frame
     */
    Object buildReadRequest(Tag tag, CommandType commandType) throws IOException;

    /**
     * Build a write request frame for single word
     */
    Object buildWriteRequest(Tag tag, CommandType commandType) throws IOException;

    /**
     * Build a write request frame for double word
     */
    Object buildWriteDoubleRequest(Tag tag) throws IOException;

    /**
     * Build a write request frame for bit
     */
    Object buildWriteBitRequest(Tag tag, boolean value) throws IOException;

    /**
     * Build a write request frame for string
     */
    Object buildWriteStringRequest(Tag tag, String strData, int theLimitToWrite) throws IOException;

    /**
     * Build a register devices request frame
     */
    Object buildRegisterRequest(List<Tag> tags, String registrationNumber) throws IOException;

    /**
     * Build an execute registered devices request frame
     */
    Object buildExecuteRequest(String registrationNumber) throws IOException;

    /**
     * Parse response and extract data as hex string
     */
    String parseResponse(Object rawResponse) throws Exception;

    /**
     * Parse response and validate it
     */
    boolean isResponseValid(Object rawResponse);

    /**
     * Get the command from response
     */
    Command getCommandFromResponse(Object rawResponse);

    /**
     * Get structured data area from response (for compatibility)
     */
    String getStructuredDataArea(Object rawResponse);

    /**
     * Check if this is a binary protocol (FENet) or text protocol (Cnet)
     */
    boolean isBinaryProtocol();
}
