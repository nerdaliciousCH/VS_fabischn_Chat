package ch.ethz.inf.vs.a3.fabischn.chat;

import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

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
import ch.ethz.inf.vs.a3.fabischn.udpclient.ConnectionResult;
import ch.ethz.inf.vs.a3.fabischn.udpclient.NetworkConsts;


// Fixing screenrotaion with AsyncTasks -> retain the instance, only ok with fragments -> wrap asynctask in fragment
// http://www.androiddesignpatterns.com/2013/04/retaining-objects-across-config-changes.html
public class MainFragment extends Fragment {

    private static final String TAG = MainFragment.class.getSimpleName();

    /**
     * Callback interface through which the fragment will report the
     * task's progress and results back to the Activity.
     */
    interface TaskCallbacks {
        void onProgressUpdate(int value);

        void onCancelled();

        void onPostExecute(ConnectionResult result);
    }

    private TaskCallbacks mCallbacks;
    private RegisterTask mRegisterTask;
    private ConnectionParameters mConnectionParameters;

    /**
     * Hold a reference to the parent Activity so we can report the
     * task's current progress and results. The Android framework
     * will pass us a reference to the newly created Activity after
     * each configuration change.
     */
    @Override
    public void onAttach(Context context) {
        // Calling this with Activity as parameter is deprecated
        // http://stackoverflow.com/questions/32083053/android-fragment-onattach-deprecated
        super.onAttach(context);
        if (context instanceof TaskCallbacks) {
            mCallbacks = (TaskCallbacks) context;
        }
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
        mRegisterTask = new RegisterTask();
        mRegisterTask.execute(mConnectionParameters);
    }

    public void cancelRegisterTask() {
        // TODO socket.close?
        mRegisterTask.cancel(true);
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

    /**
     * Note that we need to check if the callbacks are null in each
     * method in case they are invoked after the Activity's and
     * Fragment's onDestroy() method have been called.
     */

    public class RegisterTask extends AsyncTask<ConnectionParameters, Integer, ConnectionResult> {

        private final String TAG = RegisterTask.class.getSimpleName();

        private DatagramSocket socket = null;


        @Override
        public ConnectionResult doInBackground(ConnectionParameters... params) {

            String serverIPString = params[0].getServerIP();
            int serverPort = params[0].getServerPORT();
            String username = params[0].getUsername();
            String clientUUID = params[0].getClientUUID();

            InetAddress serverIP = null;
            try {
                serverIP = InetAddress.getByName(serverIPString);
            } catch (UnknownHostException e) {
                Log.e(TAG, "UNKNOWN HOST", e);
                return new ConnectionResult(false, ErrorCodes.INETADDRESS_UNKNOWN_HOST);
            }
            try {
                socket = new DatagramSocket();
                socket.setSoTimeout(NetworkConsts.SOCKET_TIMEOUT);
            } catch (SocketException e) {
                Log.e(TAG, "SETTING TIMEOUT CAUSED UDP ERROR", e);
                return new ConnectionResult(false, ErrorCodes.SOCKET_EXCEPTION);
            }

            // Exclusively send and receive to and from server
            socket.connect(serverIP, serverPort);


            // create outgoing registration packet
            // we can safely retransmit the same object multiple times
            MessageOut msgOut = new MessageOut(MessageTypes.REGISTER, username, clientUUID, null, serverIP, serverPort);
            DatagramPacket packetOut = msgOut.getDatagramPacket();

            // variables for retries & packet buffer
            boolean retry;
            int attempt = 1;
            byte[] bufIn;
            DatagramPacket packetIn = null;
            int lastError = ErrorCodes.NO_ERROR;
            // try 5 times, then stop
            while (attempt <= 5 && !isCancelled()) {
                retry = false;
                try {
                    socket.send(packetOut);
                } catch (IOException e) {
                    Log.e(TAG, "SEND FAILED!\n");
                    if (e instanceof PortUnreachableException) {
                        Log.e(TAG, "DESTINATION UNREACHABLE", e);
                        return new ConnectionResult(false, ErrorCodes.SOCKET_PORT_UNREACHABLE);
                    } else {
                        Log.e(TAG, "Something weird happened on send", e);
                    }
                }

                // Let UI thread know, that we are still trying to connect
                publishProgress(attempt);

                // create input buffer, after knowing send successful
                bufIn = new byte[NetworkConsts.PAYLOAD_SIZE];
                packetIn = new DatagramPacket(bufIn, bufIn.length);

                try {
                    socket.receive(packetIn);

                } catch (IOException e) {
                    Log.e(TAG, "RECEIVE FAILED!\n");
                    if (e instanceof SocketTimeoutException) {
                        Log.e(TAG, "SOCKET TIMEOUT");
                        lastError = ErrorCodes.SOCKET_TIMEOUT;
                        retry = true;
                        attempt++;
                    } else if (e instanceof PortUnreachableException) {
                        Log.e(TAG, "NO SERVER RUNNING AT DESTINATION", e);
                        lastError = ErrorCodes.SOCKET_PORT_UNREACHABLE;
                        retry = true;
                        attempt++;
                    } else if (e instanceof IOException) {
                        Log.e(TAG, "SOCKET EXPLODED", e);
                        return new ConnectionResult(false, ErrorCodes.SOCKET_IO_ERROR);
                    } else {
                        Log.e(TAG, "Something weird happened on receive", e);
                        return new ConnectionResult(false, ErrorCodes.UNKNOWN_ERROR);
                    }
                }

                if (!retry) {
                    // if we are here, we had no exception and really received a valid UDP packet
                    break;
                } else {
                    if (attempt > 5) {
                        return new ConnectionResult(false, lastError);
                    }
                }
            }

            if (attempt <= 5) {
            MessageIn msgIn = new MessageIn(packetIn);
            switch (msgIn.getType()) {
                case MessageTypes.ACK_MESSAGE:
                    return new ConnectionResult(true, ErrorCodes.NO_ERROR);
                case MessageTypes.ERROR_MESSAGE:
                    return new ConnectionResult(false, Integer.parseInt(msgIn.getContent()));
                default:
                    Log.e(TAG, "Switch default case. We shouldn't be here. Think harder!");
                    return new ConnectionResult(false, ErrorCodes.NO_ERROR);
            }
            } else {
                Log.e(TAG, "Server did not respond. Got 5 timeouts");
                return new ConnectionResult(false, ErrorCodes.NO_ERROR);
            }
        }


        @Override
        public void onPostExecute(ConnectionResult result) {
            if (socket != null) {
                socket.close();
            }
            if (mCallbacks != null) {
                mCallbacks.onPostExecute(result);
            }
        }

        @Override
        public void onProgressUpdate(Integer... values) {
            if (mCallbacks != null) {
                mCallbacks.onProgressUpdate(values[0]);
            }
        }

        @Override
        protected void onCancelled() {

            if (mCallbacks != null) {
                mCallbacks.onCancelled();
            }
            super.onCancelled();
        }
    }
}