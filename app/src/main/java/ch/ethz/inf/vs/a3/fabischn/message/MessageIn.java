package ch.ethz.inf.vs.a3.fabischn.message;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.Arrays;

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
        mBuffer = trim(mBuffer.clone());

        setMessage(new String(mBuffer));
        parseJSON();
        Log.d(TAG, toString());
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
            if (getType().equals(MessageTypes.ERROR_MESSAGE)) {
                setMessage(jsonBody.getString(JsonFields.BODY_CONTENT));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Couldn't parse the JSON body", e);
        }
        Log.d(TAG, "The JSON: " + mJSON.toString());
    }

    // from http://stackoverflow.com/questions/17003164/byte-array-with-padding-of-null-bytes-at-the-end-how-to-efficiently-copy-to-sma
    static byte[] trim(byte[] bytes)
    {
        int i = bytes.length - 1;
        while (i >= 0 && bytes[i] == 0)
        {
            --i;
        }

        return Arrays.copyOf(bytes, i + 1);
    }
}
