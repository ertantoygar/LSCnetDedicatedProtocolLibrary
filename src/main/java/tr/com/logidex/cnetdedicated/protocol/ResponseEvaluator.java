package tr.com.logidex.cnetdedicated.protocol;


public class ResponseEvaluator {

    private Response response;
    private String rawResponse;

    public ResponseEvaluator(String response) {
        evaluate(response);
        rawResponse = response;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    /**
     * @param res full message
     */
    private void evaluate(String res) {
        if (res.isEmpty())
            return;

        //xxx("protocol.cnetdedicated.tr.com.logidex.spreadingmachinecwithcnetprotocol.Response analyzing for the req : " + req );
        StringBuilder sb = new StringBuilder(res);

        char firstChar = sb.charAt(0);

        // Check whether ack or nak
        switch (firstChar) {
            case Response.ACK:
                response = new AckResponse();
                break;
            case Response.NAK:
                response = new NakResponse();
                break;
            default:
                response = new InvalidResponse();
                break;
        }


        //xxx(response.getClass());

        // We are interested in only the messages that start with the ACK.
        if (response instanceof AckResponse) {

            // parse station number
            char[] stNumber = new char[2];
            res.getChars(1, 3, stNumber, 0);
            response.setStationNumber(stNumber);

            // parse command
            char command = res.charAt(3);
            response.setCommand(command);

            // parse command type
            if (commandIsRorW()) {
                char[] cmdType = new char[2];
                res.getChars(4, 6, cmdType, 0);
                response.setCommandType(cmdType);
            }


            // parse data area for commands r and w.
            if (commandIsRorW()) {
                String dataAreaForRandW=res.substring(6, res.length() - 3);
                response.setStructrizedDataArea(dataAreaForRandW);
            }

            // parse data area for commands x and y.
            if (commandIsXorY()) {
                String dataAreaForXorY = res.substring(4, res.length() - 3);
                response.setStructrizedDataArea(dataAreaForXorY);
            }


        } else {
            System.err.println("Unexpected response! The system will stop!");
            System.exit(-1);
        }


    }

    private boolean commandIsRorW() {
        return response.getCommand() == Command.R || response.getCommand() == Command.W;
    }

    private boolean commandIsXorY() {
        return response.getCommand() == Command.X || response.getCommand() == Command.Y;
    }

    public Response getResponse() {
        return response;
    }




}
