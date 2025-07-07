package tr.com.logidex.cnetdedicated.protocol;
import java.util.Arrays;
public abstract class Response {
    public static final char ETX = (char) 0x03;
    public static final char ACK = (char) 0x06;
    public static final char NAK = (char) 0x15;
    private char[] stationNumber = new char[2];
    private Command command;
    private CommandType commandType = CommandType.NONE;
    private String structrizedDataArea;


    public void setStationNumber(char[] stationNumber) {
        this.stationNumber = stationNumber;
    }


    public Command getCommand() {
        return command;
    }


    public void setCommand(char commandChar) {
        char commandUpperCase = Character.toUpperCase(commandChar);
        for (Command c : Command.values()) {
            if (c.name().equals(String.valueOf(commandUpperCase))) {
                this.command = c;
                break;
            }
        }
    }


    public CommandType getCommandType() {
        return commandType;
    }


    public void setCommandType(char[] commandType) {
        String strCommandType = new String(commandType);
        for (CommandType ct : CommandType.values()) {
            if (ct.name().equals(strCommandType)) {
                this.commandType = ct;
                break;
            }
        }
    }


    public String getStructrizedDataArea() {
        return structrizedDataArea;
    }


    public void setStructrizedDataArea(String structrizedDataArea) {
        this.structrizedDataArea = structrizedDataArea;
    }


    @Override
    public String toString() {
        return "Response{" +
                getClass().getSimpleName() + ", " +
                "stationNumber=" + Arrays.toString(stationNumber) +
                ", command=" + command +
                ", commandType=" + commandType +
                ", structrizedDataArea='" + structrizedDataArea + '\'' +
                '}';
    }
}
