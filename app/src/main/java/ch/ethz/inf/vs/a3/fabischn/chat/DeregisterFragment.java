package ch.ethz.inf.vs.a3.fabischn.chat;


import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
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

import ch.ethz.inf.vs.a3.fabischn.message.ErrorCodes;
import ch.ethz.inf.vs.a3.fabischn.message.MessageIn;
import ch.ethz.inf.vs.a3.fabischn.message.MessageOut;
import ch.ethz.inf.vs.a3.fabischn.message.MessageTypes;
import ch.ethz.inf.vs.a3.fabischn.udpclient.ConnectionParameters;
import ch.ethz.inf.vs.a3.fabischn.udpclient.DeregistrationResult;
import ch.ethz.inf.vs.a3.fabischn.udpclient.NetworkConsts;

/**
 * A simple {@link Fragment} subclass.
 */
public class DeregisterFragment extends Fragment {

    private static final String TAG = DeregisterFragment.class.getSimpleName();

    /**
     * Callback interface through which the fragment will report the
     * task's progress and results back to the Activity.
     */
    interface DeregisterCallbacks {
        void onProgressUpdateDeregister(int value);

        void onCancelledDeregister();

        void onPostExecuteDeregister(DeregistrationResult result);
    }

    private DeregisterCallbacks mCallbacks;
    private DeregisterTask mDeregisterTask;
    private ConnectionParameters mConnectionParameters;
    private ChatActivity.BackActions mBackSource;

    public DeregisterFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);

        Bundle connectionParameters = this.getArguments();
        if (connectionParameters != null) {
            mConnectionParameters = (ConnectionParameters) connectionParameters.getSerializable(getString(R.string.key_connection_parameters));
            mBackSource = (ChatActivity.BackActions) connectionParameters.getSerializable(getString(R.string.key_back_source));

            // Create and execute the background task.
            mDeregisterTask = new DeregisterFragment.DeregisterTask(mBackSource);
            mDeregisterTask.execute(mConnectionParameters);
        } else {
            Log.e(TAG, "Kabooom: No bundle for fragment, no connection params");
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        TextView textView = new TextView(getActivity());
        return textView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCallbacks = (DeregisterCallbacks) context;
    }


    public void cancelDeregisterTask(){
        mDeregisterTask.cancel(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    private class DeregisterTask extends AsyncTask<ConnectionParameters, Void, DeregistrationResult> {

        private final String TAG = DeregisterTask.class.getSimpleName();

        private Context context;
        private DatagramSocket socket;
        private ChatActivity.BackActions backSource;



        public DeregisterTask(ChatActivity.BackActions backSource) {
            this.backSource = backSource;
        }

        @Override
        protected DeregistrationResult doInBackground(ConnectionParameters... params) {
            String serverIPString = params[0].getServerIP();
            int serverPort = params[0].getServerPORT();
            String username = params[0].getUsername();
            String clientUUID = params[0].getClientUUID();

            InetAddress serverIP = null;
            try {
                serverIP = InetAddress.getByName(serverIPString);
            } catch (UnknownHostException e) {
                Log.e(TAG, "UNKNOWN HOST", e);
                return new DeregistrationResult(false, backSource, ErrorCodes.INETADDRESS_UNKNOWN_HOST);
            }
            try {
                socket = new DatagramSocket();
                socket.setSoTimeout(NetworkConsts.SOCKET_TIMEOUT);
            } catch (SocketException e) {
                Log.e(TAG, "SETTING TIMEOUT CAUSED UDP ERROR", e);
                return new DeregistrationResult(false, backSource, ErrorCodes.SOCKET_EXCEPTION);
            }

            // Exclusively send and receive to and from server
            socket.connect(serverIP, serverPort);


            // create outgoing registration packet
            // we can safely retransmit the same object multiple times
            MessageOut msgOut = new MessageOut(MessageTypes.DEREGISTER, username, clientUUID, null, serverIP, serverPort);
            DatagramPacket packetOut = msgOut.getDatagramPacket();

            // variables for retries & packet buffer
            byte[] bufIn;
            DatagramPacket packetIn = null;

            try {
                socket.send(packetOut);
            } catch (IOException e) {
                Log.e(TAG, "SEND FAILED!\n");
                if (e instanceof PortUnreachableException) {
                    Log.e(TAG, "DESTINATION UNREACHABLE", e);
                    return new DeregistrationResult(false, backSource, ErrorCodes.SOCKET_PORT_UNREACHABLE);
                } else {
                    Log.e(TAG, "Something weird happened on send", e);
                }
            }

            // create input buffer, after knowing send successful
            bufIn = new byte[NetworkConsts.PAYLOAD_SIZE];
            packetIn = new DatagramPacket(bufIn, bufIn.length);

            try {
                socket.receive(packetIn);
            } catch (IOException e) {
                Log.e(TAG, "RECEIVE FAILED!\n");
                if (e instanceof SocketTimeoutException) {
                    Log.e(TAG, "SOCKET TIMEOUT");
                    return new DeregistrationResult(false, backSource, ErrorCodes.SOCKET_TIMEOUT);
                } else if (e instanceof PortUnreachableException) {
                    Log.e(TAG, "NO SERVER RUNNING AT DESTINATION", e);
                    return new DeregistrationResult(false, backSource, ErrorCodes.SOCKET_PORT_UNREACHABLE);
                } else if (e instanceof IOException) {
                    Log.e(TAG, "SOCKET EXPLODED", e);
                    return new DeregistrationResult(false, backSource, ErrorCodes.SOCKET_IO_ERROR);
                } else {
                    Log.e(TAG, "Something weird happened on receive", e);
                    return new DeregistrationResult(false, backSource, ErrorCodes.UNKNOWN_ERROR);
                }
            }

            MessageIn msgIn = new MessageIn(packetIn);
            if (msgIn.getType().equals(MessageTypes.ACK_MESSAGE)) {
                return new DeregistrationResult(true, backSource, ErrorCodes.NO_ERROR);
            } else {
                int errorCode = Integer.parseInt(msgIn.getContent());
                return new DeregistrationResult(false, backSource, errorCode);
            }
        }

        @Override
        protected void onPostExecute(DeregistrationResult result) {
            if (socket != null) {
                socket.close();
            }
            if (mCallbacks != null) {
                mCallbacks.onPostExecuteDeregister(result);
            }
        }

    }
}
