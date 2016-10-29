package ch.ethz.inf.vs.a3.fabischn.message;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.DatagramPacket;
import java.net.InetAddress;

import ch.ethz.inf.vs.a3.fabischn.udpclient.NetworkConsts;

/**
 * Created by fabian on 29.10.16.
 */

public class MessageOut extends Message {

    public static final String TAG = MessageOut.class.getSimpleName();

    private InetAddress mServerIP;
    private int mServerPORT;

    public MessageOut(final String msgType, final String username, final String uuid, final String msg, InetAddress serverIP, int serverPORT){
        super();
        setUsername(username);
        setUUID(uuid);
//        super.setTimestamp();
        setType(msgType);
        mServerIP = serverIP;
        mServerPORT = serverPORT;
        setMessage(msg);
        buildJson(msg);
    }

    private JSONObject buildJsonHeader() throws JSONException {
        JSONObject jsonHeader = new JSONObject();
        jsonHeader.put(JsonFields.HEADER_USERNAME, getUsername());
        jsonHeader.put(JsonFields.HEADER_UUID, getUUID());
        jsonHeader.put(JsonFields.HEADER_TIMESTAMP, getTimestamp());
        jsonHeader.put(JsonFields.HEADER_TYPE, getType());
        return jsonHeader;
    }

    private JSONObject buildJsonBody(final String msg) throws JSONException {

        JSONObject jsonBody = new JSONObject();

        if (getType().equals(MessageTypes.CHAT_MESSAGE)){
            jsonBody.put(JsonFields.BODY_CONTENT, msg);
        }
        return jsonBody;
    }

    private void buildJson(final String msg) {
        mJSON = new JSONObject();
        try {
            mJSON.put(JsonFields.HEADER,buildJsonHeader());
        } catch (JSONException e) {
            Log.e(TAG, "Failed writing JSON header", e);
        }
        try {
            mJSON.put(JsonFields.BODY, buildJsonBody(msg));
        } catch (JSONException e) {
            Log.e(TAG, "Failed writing JSON body", e);
        }
        Log.d(TAG, "The JSON: " + mJSON.toString());
        mBuffer = mJSON.toString().getBytes().clone();
    }

    public DatagramPacket getDatagramPacket(){
        if (mBuffer.length <= NetworkConsts.PAYLOAD_SIZE) {
            return new DatagramPacket(mBuffer, mBuffer.length, mServerIP, mServerPORT);
        }
        Log.e(TAG,"Message was too long!");
        return null;
    }


}
