package ch.ethz.inf.vs.a3.fabischn.udpclient;

/**
 * Created by fabian on 01.11.16.
 */

public class ConnectionResult {
    public static final int NO_ERROR = -1;
    private final boolean mRegistered;
    private final int mErrorCode;

    public ConnectionResult(final boolean registered, final int errorCode){
        mRegistered = registered;
        mErrorCode = errorCode;
    }

    public boolean getRegisterStatus(){
        return mRegistered;
    }

    public int getErrorCode(){
        return mErrorCode;
    }

}