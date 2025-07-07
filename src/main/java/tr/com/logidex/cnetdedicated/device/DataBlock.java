package tr.com.logidex.cnetdedicated.device;
import java.util.Arrays;
public class DataBlock {
    private char[] numberOfData = new char[2];
    private String data = "";


    public DataBlock(char[] numberOfData, String data) {
        this.numberOfData = numberOfData;
        this.data = data;
    }


    public char[] getNumberOfData() {
        return numberOfData;
    }


    public String getData() {
        return data;
    }


    @Override
    public String toString() {
        return "device.cnetdedicated.tr.com.logidex.spreadingmachinecwithcnetprotocol.DataBlock{" +
                "numberOfData=" + Arrays.toString(numberOfData) +
                ", data='" + data + '\'' +
                '}';
    }
}
