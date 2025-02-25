package tr.com.logidex.cnetdedicated.device;


import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import tr.com.logidex.cnetdedicated.util.XGBCNetUtil;

import java.math.BigInteger;

public class Tag {

    private Device device;
    private DataType dataType;
    private String address;

    private String valueAsHexString;

    private DisplayFormat displayFormat = DisplayFormat.SIGNED_INT;
    private boolean numericTag;

    private int multiplier = 1;


    private final StringProperty value = new SimpleStringProperty("0");
    private SimpleBooleanProperty dontUpdate = new SimpleBooleanProperty();


    public Tag(Device device, DataType dataType, String addr, DisplayFormat displayFormat, Integer multiplier) {

        this.device = device;
        this.dataType = dataType;
        this.address = addr;
        this.displayFormat = displayFormat;
        if (multiplier != null)
            this.multiplier = multiplier > 1 ? multiplier : 1;

        if (this.displayFormat == DisplayFormat.SIGNED_INT || this.displayFormat == DisplayFormat.FLOAT || this.displayFormat == DisplayFormat.UNSIGNED_INT) {
            numericTag = true;
        }




    }

    public StringProperty valueProperty() {
        return value;
    }

    public DataType getDataType() {
        return dataType;
    }

    public Device getDevice() {
        return device;
    }

    public DisplayFormat getDisplayFormat() {
        return displayFormat;
    }


    public void setMultiplier(int multiplier) {
        this.multiplier = multiplier;
    }

    public int getMultiplier() {
        return multiplier;
    }

    public void setValueAsHexString(String valueAsHexString) {

        this.valueAsHexString = valueAsHexString;
        switch (displayFormat) {
            case SIGNED_INT:
                toSignedInt(valueAsHexString);
                break;
            case UNSIGNED_INT:
                toUnSignedInt(valueAsHexString);
                break;
            case FLOAT:
                toFloat(valueAsHexString);
                break;
            case BINARY:
                toBinary(valueAsHexString);
                break;
            case STRING:
                toStr(valueAsHexString);
                break;
            default:
                break;
        }

    }

    private void toStr(String valueAsHexString) {
        BigInteger bigInteger = new BigInteger(valueAsHexString, 16);
        value.set(new String(bigInteger.toByteArray()).trim());

    }

    private void toBinary(String valueAsHexString) {
        if (dataType == DataType.Bit) {
            int v = Integer.parseInt(valueAsHexString, 16);
            String binaryString = Integer.toBinaryString(v);
            value.set(binaryString);
        }
        if (dataType == DataType.Word) {
            int v = Integer.parseInt(valueAsHexString, 16);
            String binaryString = Integer.toBinaryString(v);
            value.set(String.valueOf(XGBCNetUtil.addZeroToStart(16, binaryString)));
        }
        if (dataType == DataType.Dword) {
            int v = (int) Long.parseLong(valueAsHexString, 16);
            String binaryString = Integer.toBinaryString(v);
            ;
            value.set(String.valueOf(XGBCNetUtil.addZeroToStart(32, binaryString)));
        }
    }


    public String toHexString(byte[] ba) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < ba.length; i++)
            str.append(String.format("%x", ba[i]));
        return str.toString();
    }


    public String toHexString(Float floatValue) {
        int intValue = Float.floatToIntBits(floatValue * multiplier);
        return Integer.toHexString(intValue);
    }

    public String toHexString(short shortValue) {
        int intValue = Short.toUnsignedInt((short) (shortValue * multiplier));
        String hexString = Integer.toHexString(intValue);
        while (hexString.length() < 4) {
            hexString = "0" + hexString;
        }
        return hexString;
    }

    public String toHexString(Integer intValue) {
        String hexString = Integer.toHexString(intValue * multiplier);
        while (hexString.length() < 8) {
            hexString = "0" + hexString;
        }
        return hexString;
    }

    public String fromBinaryToHexString(String binaryString) {
        // Binary string'i Integer.parseInt ile integer'a çevir
        int intValue = (int) Long.parseLong(binaryString, 2);

        // Integer.toHexString ile integer'ı hexadecimal string'e çevir
        String hexString = Integer.toHexString(intValue);

        // Uygun sayıda sıfır ekleyerek istenen uzunlukta bir hex string elde et
        int targetLength = (dataType == DataType.Word) ? 4 : 8;
        hexString = XGBCNetUtil.addZeroToStart(targetLength, hexString);

        return hexString;
    }

    private void toFloat(String valueAsHexString) {

        int intValue = (int) Long.parseLong(valueAsHexString, 16);
        float floatValue = Float.intBitsToFloat(intValue);
        value.set(String.valueOf(floatValue / multiplier));


    }

    private void toUnSignedInt(String valueAsHexString) {


        if (dataType == DataType.Word) {
            int v = Integer.parseInt(valueAsHexString, 16);
            value.set(String.valueOf(v / multiplier));
        }
        if (dataType == DataType.Dword) {
            long v = Long.parseLong(valueAsHexString, 16);
            value.set(String.valueOf(v / multiplier));
        }


    }

    private void toSignedInt(String valueAsHexString) {
        if (dataType == DataType.Word) {
            short signedShortValue = (short) Integer.parseInt(valueAsHexString, 16);
            value.set(String.valueOf(signedShortValue / multiplier));
        }
        if (dataType == DataType.Dword) {
            int signedintValue = (int) Long.parseLong(valueAsHexString, 16);
            value.set(String.valueOf(signedintValue / multiplier));
        }
    }

    public String getValue() {
        return value.getValue();
    }

    public String getValueAsHexString() {
        return valueAsHexString;
    }


    public boolean isNumericTag() {
        return numericTag;
    }

    public String formatToRequest() {
        String len = XGBCNetUtil.addZeroIfNeed(this.toString().length() + 1); // +1 for sign %
        return new String(len + "%" + this.toString());
    }

    public SimpleBooleanProperty dontUpdateProperty() {
        return dontUpdate;
    }

    @Override
    public String toString() {
        StringBuilder devicePatternBuilder = new StringBuilder();
        devicePatternBuilder.append(device);
        devicePatternBuilder.append(dataType);
        devicePatternBuilder.append(address);
        return devicePatternBuilder.toString();
    }


    public void pauseUpdating() {
        
        dontUpdate.set(true);
    }

    public void resumeUpdating() {

        dontUpdate.set(false);
        
    }
}
