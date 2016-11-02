package ch.ethz.inf.vs.a3.fabischn.chat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.UUID;

import ch.ethz.inf.vs.a3.fabischn.message.ErrorCodes;
import ch.ethz.inf.vs.a3.fabischn.message.MessageIn;
import ch.ethz.inf.vs.a3.fabischn.message.MessageOut;
import ch.ethz.inf.vs.a3.fabischn.message.MessageTypes;
import ch.ethz.inf.vs.a3.fabischn.udpclient.ConnectionParameters;
import ch.ethz.inf.vs.a3.fabischn.udpclient.ConnectionResult;
import ch.ethz.inf.vs.a3.fabischn.udpclient.NetworkConsts;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener, Button.OnClickListener {


    private static final String TAG = MainActivity.class.getSimpleName();

    private static String KEY_SETTING_IP;
    private static String KEY_SETTING_PORT;

    private String mServerIP;
    private int mServerPORT;

    private String mClientUUID;
    private String mUsername;


    private EditText mEditTextUsername;
    private Button mButtonJoin;
    private RelativeLayout mLayoutProgressBar;

    private SharedPreferences mSharedPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        KEY_SETTING_IP = getString(R.string.setting_ip);
        KEY_SETTING_PORT = getString(R.string.setting_port);

        mEditTextUsername = (EditText) findViewById(R.id.edittext_username);
        mButtonJoin = (Button) findViewById(R.id.btn_join);
        mLayoutProgressBar = (RelativeLayout) findViewById(R.id.layout_main_loading_bar);
        mLayoutProgressBar.setVisibility(View.GONE);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // TODO check wifi connection
    }

    @Override
    protected void onResume() {
        // TODO test lifecycle
        super.onResume();
        Log.d(TAG, "onResume");
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
        mServerIP = getSettingsIP();
        mServerPORT = getSettingsPORT();
        updateJoinButtonServerAddress();
        enableUI();

        // TODO RESUME ASYNCTASK
    }

    @Override
    protected void onPause() {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
        // TODO KILL ASYNCTASK
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                Intent myIntent = new Intent(this, SettingsActivity.class);
                this.startActivity(myIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key == KEY_SETTING_IP) {
            mServerIP = getSettingsIP();
            updateJoinButtonServerAddress();
        }
        if (key == KEY_SETTING_PORT) {
            mServerPORT = getSettingsPORT();
            updateJoinButtonServerAddress();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_join) {

            mUsername = mEditTextUsername.getText().toString();
            if (!(mUsername.equals("") || mUsername.contains("\n") || mUsername.contains("\t") || mUsername.contains(" "))) {
                mButtonJoin.setText(getString(R.string.trying_connect));
                disableUI();
                // TODO set wakelock?
                // https://developer.android.com/training/scheduling/wakelock.html
                mClientUUID = UUID.randomUUID().toString();
                ServerConnectionTask connectToServer = new ServerConnectionTask(this);
                connectToServer.execute(new ConnectionParameters(mServerIP, mServerPORT, mClientUUID, mUsername));
            } else {
                Toast.makeText(this, "Invalid username: Must be non empty and not contain spaces, tabs or newlines", Toast.LENGTH_SHORT).show();
            }

        }
    }

    public void updateJoinButtonServerAddress() {
        mButtonJoin.setText("Join Chat @ " + mServerIP + ":" + mServerPORT);
    }

    public void disableUI(){
        mLayoutProgressBar.setVisibility(View.VISIBLE);
        mButtonJoin.setEnabled(false);
        mEditTextUsername.setEnabled(false);
    }

    public void enableUI(){
        mLayoutProgressBar.setVisibility(View.GONE);
        mButtonJoin.setEnabled(true);
        mEditTextUsername.setEnabled(true);
    }


    public String getSettingsIP(){
        return mSharedPreferences.getString(KEY_SETTING_IP, "no ip");
    }

    public int getSettingsPORT(){
        return Integer.parseInt(mSharedPreferences.getString(KEY_SETTING_PORT, "0"));
    }

    public class ServerConnectionTask extends AsyncTask<ConnectionParameters, Integer, ConnectionResult> {

        // TODO AsyncTask and Screenrotation :(
        private final String TAG = ServerConnectionTask.class.getSimpleName();

        private Context context;

        private DatagramSocket socket = null;

        public ServerConnectionTask(Context context) {
            this.context = context;
        }

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
            while (attempt <= 5) {
                retry = false;
                try {
                    socket.send(packetOut);
                } catch (IOException e) {
                    Log.e(TAG, "SEND FAILED!\n");
                    if (e instanceof PortUnreachableException){
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
                    Log.e(TAG,"RECEIVE FAILED!\n");
                    if (e instanceof SocketTimeoutException) {
                        Log.e(TAG, "SOCKET TIMEOUT");
                        lastError = ErrorCodes.SOCKET_TIMEOUT;
                        retry = true;
                        attempt++;
                    } else if(e instanceof PortUnreachableException) {
                        Log.e(TAG, "NO SERVER RUNNING AT DESTINATION", e);
                        lastError = ErrorCodes.SOCKET_PORT_UNREACHABLE;
                        retry = true;
                        attempt++;
//                        return new ConnectionResult(false, ErrorCodes.SOCKET_PORT_UNREACHABLE);
                    } else if(e instanceof IOException){
                        Log.e(TAG, "SOCKET EXPLODED", e);
                        return new ConnectionResult(false, ErrorCodes.SOCKET_IO_ERROR);
                    } else{
                        Log.e(TAG, "Something weird happened on receive", e);
                        return new ConnectionResult(false, ErrorCodes.UNKNOWN_ERROR);
                    }
                }

                if (!retry) {
                    // if we are here, we had no exception and really received a valid UDP packet
                    break;
                } else{
                    if(attempt >5){
                        return new ConnectionResult(false, lastError);
                    }
                }
            }

//            if (attempt <= 5) {
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
//            } else {
//                Log.e(TAG, "Server did not respond. Got 5 timeouts");
//                return new ConnectionResult(false, ConnectionResult.NO_ERROR);
//            }
        }


        @Override
        public void onPostExecute(ConnectionResult result) {
            socket.close();
            if (result.getRegisterStatus()){
                Toast.makeText(context, "Registration succesfull!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("username", mUsername);
                intent.putExtra("uuid", mClientUUID);
                startActivity(intent);
            } else {
                // TODO unset wakelock
                // https://developer.android.com/training/scheduling/wakelock.html
                int errorCode = result.getErrorCode();
                String errorMessage = "Unknown error";
                if (errorCode >= 0 && errorCode < 7){
                    // the protocol error codes and my own errorcodes
                    errorMessage = ErrorCodes.getStringError(errorCode);
                }
                Toast.makeText(context, "Couldn't join the chat: " + errorMessage, Toast.LENGTH_SHORT).show();
                updateJoinButtonServerAddress();
                enableUI();
            }
        }

        @Override
        public void onProgressUpdate(Integer... values) {

            // TODO fix the numbers
            switch (values[0].intValue()) {
                case 1:
                    mButtonJoin.setText(getString(R.string.trying_connect));
                    break;
                case 2:
                    mButtonJoin.setText(getString(R.string.retry_1));
                    break;
                case 3:
                    mButtonJoin.setText(getString(R.string.retry_2));
                    break;
                case 4:
                    mButtonJoin.setText(getString(R.string.retry_3));
                    break;
                case 5:
                    mButtonJoin.setText(getString(R.string.retry_4));
                default:
                    break;
            }
        }
    }

}
