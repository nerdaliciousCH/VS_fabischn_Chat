package ch.ethz.inf.vs.a3.fabischn.udpclient;

/**
 * Created by fabian on 31.10.16.
 */
public class ConnectionParameters {

    private final String mServerIP;
    private final int mServerPORT;
    private final String mClientUUID;
    private final String mUsername;

    public ConnectionParameters(final String serverIP, final int serverPORT, final String clientUUID, final String username) {
        mServerIP = serverIP;
        mServerPORT = serverPORT;
        mClientUUID = clientUUID;
        mUsername = username;
    }

    public String getServerIP() {
        return mServerIP;
    }

    public int getServerPORT() {
        return mServerPORT;
    }

    public String getClientUUID() {
        return mClientUUID;
    }

    public String getUsername() {
        return mUsername;
    }
}