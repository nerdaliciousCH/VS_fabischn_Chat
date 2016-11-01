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
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.UUID;

import ch.ethz.inf.vs.a3.fabischn.message.ErrorCodes;
import ch.ethz.inf.vs.a3.fabischn.message.MessageIn;
import ch.ethz.inf.vs.a3.fabischn.message.MessageOut;
import ch.ethz.inf.vs.a3.fabischn.message.MessageTypes;
import ch.ethz.inf.vs.a3.fabischn.udpclient.ConnectionParameters;
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

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);

        // TODO maybe we are connecting? Button might show something else...
        mServerIP = getSettingsIP();
        mServerPORT = getSettingsPORT();
        updateJoinButtonServerAddress();
    }

    @Override
    protected void onPause() {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
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

            // TODO maybe nicer UI, some kind of rotating busy sign?
            // TODO set it on OK of edittext instead of this ugly piece...
            mUsername = mEditTextUsername.getText().toString();

            // TODO regex match the name
            if (!(mUsername.equals("") || mUsername.contains("\n") || mUsername.contains("\t"))) {
                mButtonJoin.setText(getString(R.string.trying_connect));
                disableUI();

                mClientUUID = UUID.randomUUID().toString();
                ServerConnectionTask connectToServer = new ServerConnectionTask(this);
                connectToServer.execute(new ConnectionParameters(mServerIP, mServerPORT, mClientUUID, mUsername));
            } else {
                Toast.makeText(this, "Username should not be empty, or contain spaces, or span several lines", Toast.LENGTH_LONG).show();
            }

        }
    }

    public void updateJoinButtonServerAddress() {
        mButtonJoin.setText("Join Chat @ " + mServerIP + ":" + mServerPORT);
    }

    public void disableUI(){
        mButtonJoin.setEnabled(false);
        mEditTextUsername.setEnabled(false);
    }

    public void resetUI(){
        mButtonJoin.setText(getString(R.string.join_chat));
    }

    public void enableUI(){
        mButtonJoin.setEnabled(true);
        mEditTextUsername.setEnabled(true);
    }


    public String getSettingsIP(){
        // TODO sanity checks
        return mSharedPreferences.getString(KEY_SETTING_IP, "no ip");
    }

    public int getSettingsPORT(){
        // TODO sanity checks
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
                Log.e(TAG, "Unknown Host, couldn't parse IP", e);
                return new ConnectionResult(false, ConnectionResult.NO_ERROR);
            }
            try {
                socket = new DatagramSocket();
                socket.setSoTimeout(NetworkConsts.SOCKET_TIMEOUT);
            } catch (SocketException e) {
                Log.e(TAG, "Couldn't create UDP socket", e);
                return new ConnectionResult(false, ConnectionResult.NO_ERROR);
            }

            // Exclusively send and receive to and from server
            socket.connect(serverIP, serverPort);


            // create outgoing registration packet
            // we can safely retransmit the same object multiple times
            MessageOut msgOut = new MessageOut(MessageTypes.REGISTER, username, clientUUID, null, serverIP, serverPort);
            DatagramPacket packetOut = msgOut.getDatagramPacket();

            // variables for retries & packet buffer
            boolean gotTimeout;
            int tries = 0;
            byte[] bufIn;
            DatagramPacket packetIn = null;

            // try 5 times, then stop
            while (tries < 5) {
                gotTimeout = false;
                try {
                    socket.send(packetOut);
                } catch (IOException e) {
                    Log.e(TAG, "Couldn't send", e);
                    return new ConnectionResult(false, ConnectionResult.NO_ERROR);
                }
                tries++;

                // Let UI thread know, that we are still trying to connect
                publishProgress(tries);

                // TODO can we recycle it?
                // create input buffer, after knowing send successful
                bufIn = new byte[NetworkConsts.PAYLOAD_SIZE];
                packetIn = new DatagramPacket(bufIn, bufIn.length);

                try {
                    socket.receive(packetIn);
                } catch (IOException e) {
                    if (e instanceof SocketTimeoutException) {
                        Log.d(TAG, "Socket TIMEOUT");
                        gotTimeout = true;
                    } else {
                        Log.e(TAG, "Couldn't receive", e);
                        return new ConnectionResult(false, ConnectionResult.NO_ERROR);
                    }
                }
                if (!gotTimeout) {
                    // if we are here, we had no exception and received a valid UDP packet
                    break;
                }
            }

            if (tries < 5) {
                MessageIn msgIn = new MessageIn(packetIn);
                switch (msgIn.getType()) {
                    case MessageTypes.ACK_MESSAGE:
                        return new ConnectionResult(true, ConnectionResult.NO_ERROR);
                    case MessageTypes.ERROR_MESSAGE:
                        return new ConnectionResult(false, Integer.parseInt(msgIn.getMessage()));
                    default:
                        Log.e(TAG, "Switch default case. We shouldn't be here. Think harder!");
                        return new ConnectionResult(false, ConnectionResult.NO_ERROR);
                }
            } else {
                Log.e(TAG, "Server did not respond. Got 5 timeouts");
                return new ConnectionResult(false, ConnectionResult.NO_ERROR);
            }
        }


        @Override
        public void onPostExecute(ConnectionResult result) {
            socket.close();
            if (result.getRegisterStatus()){
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("username", mUsername);
                intent.putExtra("uuid", mClientUUID);
                startActivity(intent);
            } else {
                int errorCode = result.getErrorCode();
                String errorMessage = "";
                if (errorCode >= 0 && errorCode < 5){
                    errorMessage = ErrorCodes.getStringError(errorCode);
                } else{
                    // TODO custom errors, maybe distinct between several network errors?
                    errorMessage = "Communication failed";
                }
                Toast.makeText(context, "Couldn't join the chat: " + errorMessage, Toast.LENGTH_LONG).show();
                resetUI();
                enableUI();
            }
        }

        @Override
        public void onProgressUpdate(Integer... values) {

            // TODO maybe nicer UI, some kind of rotating busy sign?
            switch (values[0].intValue()) {
                case 1:
                    mButtonJoin.setText(getString(R.string.reconnect_one));
                    Log.d(TAG, "reconnect 1");
                    break;
                case 2:
                    mButtonJoin.setText(getString(R.string.reconnect_two));
                    Log.d(TAG, "reconnect 2");
                    break;
                case 3:
                    mButtonJoin.setText(getString(R.string.reconnect_three));
                    Log.d(TAG, "reconnect 3");
                    break;
                case 4:
                    mButtonJoin.setText(getString(R.string.reconnect_four));
                    Log.d(TAG, "reconnect 4");
                    break;
                default:
                    Log.d(TAG, ">4 tries currently");
                    break;
            }
        }
    }

    private class ConnectionResult {
        private static final int NO_ERROR = -1;
        private final boolean mRegistered;
        private final int mErrorCode;

        private ConnectionResult(final boolean registered, final int errorCode){
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

}
