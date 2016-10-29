package ch.ethz.inf.vs.a3.fabischn.message;

import org.json.JSONObject;

/**
 * Created by fabian on 28.10.16.
 */

public abstract class Message {

    private String mUsername;
    private String mUUID;
    private String mTimestamp;
    private String mType;

    private String mMessage;

    protected byte[] mBuffer;
    protected JSONObject mJSON;

    public Message(){
        mUsername = "";
        mUUID = "";
        mTimestamp = "{}";
        mType = "";
    }




    @Override
    public String toString() {
        if (mJSON != null){
            return new String(mJSON.toString());
        }
        else{
            return new String("JSONObject was null");
        }
    }

    protected String getUsername() {
        return mUsername;
    }

    protected void setUsername(String mUsername) {
        this.mUsername = mUsername;
    }

    protected String getUUID() {
        return mUUID;
    }

    protected void setUUID(String mUUID) {
        this.mUUID = mUUID;
    }

    protected String getTimestamp() {
        return mTimestamp;
    }

    protected void setTimestamp(String mTimestamp) {
        this.mTimestamp = mTimestamp;
    }

    protected String getType() {
        return mType;
    }

    protected void setType(String mMessageType) {
        this.mType = mMessageType;
    }

    protected String getMessage(){
        return mMessage;
    }

    protected void setMessage(String mMessage){
        this.mMessage = mMessage;
    }
}
