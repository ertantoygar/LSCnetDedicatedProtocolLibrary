package tr.com.logidex.cnetdedicated.protocol.connection;
import tr.com.logidex.cnetdedicated.protocol.SerialConnectionParams;

import java.io.Serial;
public class ConnectionFactory {

    public static Connection createConnection(ConnectionParams params){

        if(params instanceof SerialConnectionParams){
            return new SerialConnection((SerialConnectionParams)params);
        }
        else{
            throw new IllegalArgumentException("Unsopported connection params type: " + params.getClass().getName());
        }

    }
}
