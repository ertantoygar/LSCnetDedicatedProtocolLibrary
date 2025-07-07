package tr.com.logidex.cnetdedicated.protocol;
public enum CommandType {
    /**
     * Individual
     * Read or write direct variable of Bit,Byte,Word,Dword,Lword.
     */
    SS,
    /**
     * Continuous
     * Read or write direct variable of Bit,Byte,Word,Dword,Lword type with block unit.
     */
    SB,
    /**
     * Individual reading of device
     */
    RSS,
    /**
     * Continuous reading of device
     */
    RSB,
    NONE,
}
