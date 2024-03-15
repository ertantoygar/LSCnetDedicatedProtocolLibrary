package tr.com.logidex.cnetdedicated.device;

public enum DataType {
    Bit('X'),
    Byte('B'),
    Word('W'),
    Dword('D'),
    Lword('L');

    private final char markingChar;

    DataType(char c) {
        this.markingChar = c;
    }


    @Override
    public String toString() {
        return String.valueOf(markingChar);

    }
}
