package tr.com.logidex.cnetdedicated.protocol.fenet;

import tr.com.logidex.cnetdedicated.protocol.Command;

/**
 * Represents a parsed FENet response from the PLC
 */
public class FENetResponse {

    private byte[] companyId; // 10 bytes
    private byte[] plcInfo; // 2 bytes
    private byte cpuInfo; // 1 byte
    private byte frameDirection; // 1 byte (0x11 for response)
    private int invokeId; // 2 bytes
    private int length; // 2 bytes
    private byte positionInfo; // 1 byte
    private byte checksum; // 1 byte

    // Payload fields
    private short instruction; // 2 bytes (0x5500 for read response, 0x5900 for write response)
    private byte dataType; // 1 byte
    private byte reserved; // 1 byte
    private short numberOfBlocks; // 2 bytes
    private short errorStatus; // 2 bytes (0x0000 = success)
    private byte[] data; // Variable length

    private boolean valid = false;
    private String errorMessage;

    public FENetResponse() {
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isSuccess() {
        return valid && errorStatus == 0x0000;
    }

    public boolean isReadResponse() {
        return instruction == 0x5500;
    }

    public boolean isWriteResponse() {
        return instruction == 0x5900;
    }

    public Command getCommand() {
        if (instruction == 0x5500) {
            return Command.R;
        } else if (instruction == 0x5900) {
            return Command.W;
        }
        return null;
    }

    // Getters and setters

    public byte[] getCompanyId() {
        return companyId;
    }

    public void setCompanyId(byte[] companyId) {
        this.companyId = companyId;
    }

    public byte[] getPlcInfo() {
        return plcInfo;
    }

    public void setPlcInfo(byte[] plcInfo) {
        this.plcInfo = plcInfo;
    }

    public byte getCpuInfo() {
        return cpuInfo;
    }

    public void setCpuInfo(byte cpuInfo) {
        this.cpuInfo = cpuInfo;
    }

    public byte getFrameDirection() {
        return frameDirection;
    }

    public void setFrameDirection(byte frameDirection) {
        this.frameDirection = frameDirection;
    }

    public int getInvokeId() {
        return invokeId;
    }

    public void setInvokeId(int invokeId) {
        this.invokeId = invokeId;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public byte getPositionInfo() {
        return positionInfo;
    }

    public void setPositionInfo(byte positionInfo) {
        this.positionInfo = positionInfo;
    }

    public byte getChecksum() {
        return checksum;
    }

    public void setChecksum(byte checksum) {
        this.checksum = checksum;
    }

    public short getInstruction() {
        return instruction;
    }

    public void setInstruction(short instruction) {
        this.instruction = instruction;
    }

    public byte getDataType() {
        return dataType;
    }

    public void setDataType(byte dataType) {
        this.dataType = dataType;
    }

    public byte getReserved() {
        return reserved;
    }

    public void setReserved(byte reserved) {
        this.reserved = reserved;
    }

    public short getNumberOfBlocks() {
        return numberOfBlocks;
    }

    public void setNumberOfBlocks(short numberOfBlocks) {
        this.numberOfBlocks = numberOfBlocks;
    }

    public short getErrorStatus() {
        return errorStatus;
    }

    public void setErrorStatus(short errorStatus) {
        this.errorStatus = errorStatus;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    /**
     * Get data as hex string (compatible with existing Tag.setValueAsHexString)
     */
    public String getDataAsHexString() {
        if (data == null || data.length == 0) {
            return "";
        }

        StringBuilder hex = new StringBuilder();
        // FENet returns data in little-endian, convert to big-endian hex string
        for (int i = data.length - 1; i >= 0; i--) {
            hex.append(String.format("%02X", data[i] & 0xFF));
        }
        return hex.toString();
    }

    /**
     * Get individual block data as hex string for batch reads
     * @param blockIndex The index of the block to extract
     * @param dataLength The expected length of each block in bytes
     * @return Hex string for the specific block
     */
    public String getBlockDataAsHexString(int blockIndex, int dataLength) {
        if (data == null || data.length == 0) {
            return "";
        }

        int startOffset = blockIndex * dataLength;
        int endOffset = Math.min(startOffset + dataLength, data.length);

        if (startOffset >= data.length) {
            return "";
        }

        StringBuilder hex = new StringBuilder();
        // FENet returns data in little-endian, convert to big-endian hex string
        for (int i = endOffset - 1; i >= startOffset; i--) {
            hex.append(String.format("%02X", data[i] & 0xFF));
        }
        return hex.toString();
    }

    @Override
    public String toString() {
        return String.format("FENetResponse{valid=%s, instruction=0x%04X, errorStatus=0x%04X, invokeId=%d, dataLength=%d}",
                valid, instruction, errorStatus, invokeId, data != null ? data.length : 0);
    }
}
