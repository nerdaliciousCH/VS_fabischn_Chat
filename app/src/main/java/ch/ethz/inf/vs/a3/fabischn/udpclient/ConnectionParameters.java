package ch.ethz.inf.vs.a3.fabischn.udpclient;

/**
 * Created by fabian on 31.10.16.
 */
public class ConnectionParameters{

    private String mServerIP;
    private int mServerPORT;
    private String mClientUUID;
    private String mUsername;

    public ConnectionParameters(String serverIP, int serverPORT, String clientUUID, String username){
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