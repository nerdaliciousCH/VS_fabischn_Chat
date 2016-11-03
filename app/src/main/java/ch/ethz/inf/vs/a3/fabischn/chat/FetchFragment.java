package ch.ethz.inf.vs.a3.fabischn.chat;


import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import ch.ethz.inf.vs.a3.fabischn.message.Message;
import ch.ethz.inf.vs.a3.fabischn.message.MessageComparator;
import ch.ethz.inf.vs.a3.fabischn.message.MessageIn;
import ch.ethz.inf.vs.a3.fabischn.message.MessageOut;
import ch.ethz.inf.vs.a3.fabischn.message.MessageTypes;
import ch.ethz.inf.vs.a3.fabischn.queue.PriorityQueue;
import ch.ethz.inf.vs.a3.fabischn.udpclient.ConnectionParameters;
import ch.ethz.inf.vs.a3.fabischn.udpclient.NetworkConsts;

/**
 * A simple {@link Fragment} subclass.
 */
public class FetchFragment extends Fragment {

    private static final String TAG = FetchFragment.class.getSimpleName();

    /**
     * Callback interface through which the fragment will report the
     * task's progress and results back to the Activity.
     */
    interface FetchCallbacks {
        void onProgressUpdateFetch(int value);

        void onCancelledFetch();

        void onPostExecuteFetch(PriorityQueue<Message> messageIns);
    }

    private FetchCallbacks mCallbacks;
    private FetchTask mFetchTask;
    private ConnectionParameters mConnectionParameters;

    public FetchFragment() {

    }

    @Override
    public void onAttach(Context context) {
        // Calling this with Activity as parameter is deprecated
        // http://stackoverflow.com/questions/32083053/android-fragment-onattach-deprecated
        super.onAttach(context);
        mCallbacks = (FetchCallbacks) context;

    }

    /**
     * This method will only be called once when the retained
     * Fragment is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);

        Bundle connectionParameters = this.getArguments();
        if (connectionParameters != null) {
            mConnectionParameters = (ConnectionParameters) connectionParameters.getSerializable(getString(R.string.key_connection_parameters));
        } else {
            Log.e(TAG, "Kabooom: No bundle for fragment, no connection params");
        }

        // Create and execute the background task.
        mFetchTask = new FetchTask();
        mFetchTask.execute(mConnectionParameters);
    }

    public void cancelFetchTask() {
        mFetchTask.cancel(true);
    }

    /**
     * Set the callback to null so we don't accidentally leak the
     * Activity instance.
     */
    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    private class FetchTask extends AsyncTask<ConnectionParameters, Integer, PriorityQueue<Message>> {

        private final String TAG = FetchTask.class.getSimpleName();

        private DatagramSocket socket = null;


        // TODO wrap the queue in an object that can hold an errorcode
        @Override
        protected PriorityQueue<Message> doInBackground(ConnectionParameters... params) {

            String serverIPString = params[0].getServerIP();
            int serverPort = params[0].getServerPORT();
            String username = params[0].getUsername();
            String clientUUID = params[0].getClientUUID();

            InetAddress serverIP = null;
            try {
                serverIP = InetAddress.getByName(serverIPString);
            } catch (UnknownHostException e) {
                Log.e(TAG, "UNKOWN_HOST", e);
                return null;
            }
            try {
                socket = new DatagramSocket();
                socket.setSoTimeout(NetworkConsts.SOCKET_TIMEOUT);
            } catch (SocketException e) {
                Log.e(TAG, "SETTING TIMEOUT CAUSED UDP ERROR", e);
                return null;
            }

            // Exclusively send and receive to and from server
            socket.connect(serverIP, serverPort);

            MessageOut msgOut = new MessageOut(MessageTypes.RETRIEVE_CHAT_LOG, username, clientUUID, null, serverIP, serverPort);
            DatagramPacket packetOut = msgOut.getDatagramPacket();
            try {
                socket.send(packetOut);
            } catch (IOException e) {
                Log.e(TAG, "SEND FAILED!\n");
                if (e instanceof PortUnreachableException) {
                    Log.e(TAG, "DESTINATION UNREACHABLE", e);
                } else {
                    Log.e(TAG, "Something weird happened on send", e);
                }
                return null;
            }

            PriorityQueue<Message> messages = new PriorityQueue<Message>(new MessageComparator());

            byte[] bufIn;
            DatagramPacket packetIn;

            while (true) {
                bufIn = new byte[NetworkConsts.PAYLOAD_SIZE];
                packetIn = new DatagramPacket(bufIn, bufIn.length);
                try {
                    socket.receive(packetIn);
                    messages.add(new MessageIn(packetIn));
                } catch (IOException e) {
                    if (e instanceof SocketTimeoutException) {
                        Log.e(TAG, "Socket timed out trying to receive");
                        return messages;
                    } else if (e instanceof PortUnreachableException) {
                        Log.e(TAG, "NO SERVER RUNNING AT DESTINATION", e);
                    } else if (e instanceof IOException) {
                        Log.e(TAG, "SOCKET EXPLODED", e);
                    } else {
                        Log.e(TAG, "Something weird happened on receive", e);
                    }
                    return null;
                }
            }
        }

        @Override
        protected void onPostExecute(PriorityQueue<Message> messageIns) {
            if (socket != null) {
                socket.close();
            }
            if(mCallbacks != null) {
                mCallbacks.onPostExecuteFetch(messageIns);
            }
        }

        // TODO onCancel? onPreExecute? onProgressUpdate?

    }

}
