package tr.com.logidex.cnetdedicated.device;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * protocol.cnetdedicated.tr.com.logidex.spreadingmachinecwithcnetprotocol.Response after execution of command (XGB->PC)
 */
public class RegisteredDataBlock {
    private char[] registrationNumber = new char[2];
    private char[] numberOfBlocks = new char[2];
    private List<DataBlock> dataBlocks = new ArrayList<>();


    public char[] getRegistrationNumber() {
        return registrationNumber;
    }


    public void setRegistrationNumber(char[] registrationNumber) {
        this.registrationNumber = registrationNumber;
    }


    public char[] getNumberOfBlocks() {
        return numberOfBlocks;
    }


    public void setNumberOfBlocks(char[] numberOfBlocks) {
        this.numberOfBlocks = numberOfBlocks;
    }


    public List<DataBlock> getDataBlocks() {
        return dataBlocks;
    }


    public void setDataBlocks(List<DataBlock> dataBlocks) {
        this.dataBlocks = dataBlocks;
    }


    @Override
    public String toString() {
        return "device.cnetdedicated.tr.com.logidex.spreadingmachinecwithcnetprotocol.RegisteredDataBlock{" +
                "registrationNumber=" + Arrays.toString(registrationNumber) +
                ", numberOfBlocks=" + Arrays.toString(numberOfBlocks) +
                ", dataBlocks=" + dataBlocks +
                '}';
    }
}
