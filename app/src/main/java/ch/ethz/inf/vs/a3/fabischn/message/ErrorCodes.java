package ch.ethz.inf.vs.a3.fabischn.message;

public class ErrorCodes {

    public static final int NO_ERROR = -1;

    public static final int REG_FAIL = 0;

    public static final int DEREG_FAIL_UUID = 1;

    public static final int DEREG_FAIL_USERNAME = 2;

    public static final int USER_AUTHENTICATION_FAIL = 3;

    public static final int MSG_PARSING_FAILED = 4;

    public static final int SOCKET_PORT_UNREACHABLE = 5;

    public static final int SOCKET_TIMEOUT = 6;

    public static final int SOCKET_IO_ERROR = 7;

    public static final int SOCKET_EXCEPTION = 8;

    public static final int INETADDRESS_UNKNOWN_HOST = 9;

    public static final int USER_CANCELLED = 10;

    public static final int UNKNOWN_ERROR = 11;


    public static String getStringError(int errorCode) {
        String error = null;
        switch (errorCode) {
            case -1:
                error = "Everything went fine";
                break;

            case 0:
                error = "Registration failed";
                break;

            case 1:
                error = "Deregistration failed due to the UUID";
                break;

            case 2:
                error = "Deregistration failed due to the username";
                break;

            case 3:
                error = "User authentication fail";
                break;

            case 4:
                error = "Message parsing failed";
                break;


            case 5:
                error = "There is no chat server at destination";
                break;

            case 6:
                error = "The connection timed out";
                break;
            case 7:
            case 8:
                error = "Couldn't setup a connection to destination";
                break;

            case 9:
                error = "IP address was malformed";
                break;

            case 10:
                error = "Connection aborted";
                break;

            case 11:
                error = "Can't tell what actually went wrong";
                break;

            default:
                error = "Cannot decode error code";
                break;
        }

        return error;
    }
}
