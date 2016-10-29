package ch.ethz.inf.vs.a3.fabischn.message;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.DatagramPacket;

import ch.ethz.inf.vs.a3.fabischn.udpclient.NetworkConsts;

/**
 * Created by fabian on 29.10.16.
 */

public class MessageIn extends Message {

    private static final String TAG = MessageIn.class.getSimpleName();

    public MessageIn(DatagramPacket packet){
        super();
        mBuffer = packet.getData().clone();
        if (mBuffer.length > NetworkConsts.PAYLOAD_SIZE){
            //TODO throw exception?
        }
        setMessage(mBuffer.toString());
        parseJSON();
    }

    private void parseJSON(){
        try {
            mJSON = new JSONObject(getMessage());
        } catch (JSONException e) {
            Log.e(TAG, "Couldn't parse the String to JSON", e);
            mJSON = null;
            return;
        }
        try {
            JSONObject jsonHeader = mJSON.getJSONObject(JsonFields.HEADER);
            setUsername(jsonHeader.getString(JsonFields.HEADER_USERNAME));
            setUUID(jsonHeader.getString(JsonFields.HEADER_UUID));
            setTimestamp(jsonHeader.getString(JsonFields.HEADER_TIMESTAMP));
            setType(jsonHeader.getString(JsonFields.HEADER_TYPE));
        } catch (JSONException e) {
            Log.e(TAG, "Couldn't parse the JSON header", e);
        }
        try {
            JSONObject jsonBody = mJSON.getJSONObject(JsonFields.BODY);
            setMessage(jsonBody.getString(JsonFields.BODY_CONTENT));
        } catch (JSONException e) {
            Log.e(TAG, "Couldn't parse the JSON body", e);
        }
    }
}
