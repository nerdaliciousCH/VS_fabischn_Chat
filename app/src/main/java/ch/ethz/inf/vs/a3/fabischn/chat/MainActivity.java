package ch.ethz.inf.vs.a3.fabischn.chat;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import java.util.concurrent.ExecutionException;

import ch.ethz.inf.vs.a3.fabischn.message.MessageIn;
import ch.ethz.inf.vs.a3.fabischn.message.MessageOut;
import ch.ethz.inf.vs.a3.fabischn.message.MessageTypes;
import ch.ethz.inf.vs.a3.fabischn.udpclient.NetworkConsts;
import ch.ethz.inf.vs.a3.fabischn.udpclient.UDPClient;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener, Button.OnClickListener {


    private static final String TAG = MainActivity.class.getSimpleName();

    private static String KEY_SETTING_IP;
    private static String KEY_SETTING_PORT;

    private String mChatServerIPString;
    private String mChatServerPORTString;
    private int mChatServerPORTInteger;

    private EditText mEditTextUsername;
    private Button mButtonJoin;

    private SharedPreferences mSharedPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        KEY_SETTING_IP = getResources().getString(R.string.setting_ip);
        KEY_SETTING_PORT = getResources().getString(R.string.setting_port);

        mEditTextUsername = (EditText) findViewById(R.id.edittext_username);
        mButtonJoin = (Button) findViewById(R.id.btn_join);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

//        Log.d(TAG, "onCreate");
//        if (savedInstanceState != null) {
//            Log.d(TAG,"bundle is here");
//            String text = savedInstanceState.getString("username");
//            Log.d(TAG, "loaded username: " + text);
//            if (text != null && !text.equals("")) {
//                mEditTextUsername.setText(text);
//            }
//        } else{
//            Log.d(TAG,"bundle null");
//        }

    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
        mChatServerIPString = mSharedPreferences.getString(KEY_SETTING_IP, "no ip");
        mChatServerPORTString = mSharedPreferences.getString(KEY_SETTING_PORT, "no port");

        mChatServerPORTInteger = Integer.parseInt(mChatServerPORTString);
        updateJoinButton();
    }

    @Override
    protected void onPause() {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

//    @Override
//    protected void onRestoreInstanceState(Bundle savedInstanceState) {
//        super.onRestoreInstanceState(savedInstanceState);
//        Log.d(TAG,"onRestoreInstanceState");
//        if (savedInstanceState != null) {
//            Log.d(TAG,"bundle is here");
//            String text = savedInstanceState.getString("username");
//            Log.d(TAG, "loaded username: " + text);
//            if (text != null && !text.equals("")) {
//                mEditTextUsername.setText(text);
//            }
//        } else{
//            Log.d(TAG,"bundle null");
//        }
//
//    }

//    @Override
//    protected void onSaveInstanceState(Bundle outState) {
//        super.onSaveInstanceState(outState);
//        Log.d(TAG,"onSaveInstanceState");
//        outState.putString("username", mEditTextUsername.getText().toString());
//    }

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
            mChatServerIPString = mSharedPreferences.getString(KEY_SETTING_IP, "no ip");
            updateJoinButton();
        }

        if (key == KEY_SETTING_PORT) {
            mChatServerPORTString = mSharedPreferences.getString(KEY_SETTING_PORT, "no port");
            updateJoinButton();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_join) {
            String username = mEditTextUsername.getText().toString();

            // TODO maybe safe alread used and registered pairs of usernames and UUIDs for reconnecting?
            if (!username.equals("")) {
                String clientUUID = UUID.randomUUID().toString();
                ServerConnectionTask connectToServer = new ServerConnectionTask();
                connectToServer.execute(new ConnectionParameters(mChatServerIPString, mChatServerPORTInteger, clientUUID, username));
                boolean registered = false;
                try {
                    registered = connectToServer.get();

                } catch (InterruptedException e) {
                    // TODO Handle this correctly
                    Log.e(TAG, "Got interrupted", e);
                } catch (ExecutionException e) {
                    Log.e(TAG, "Exploded during execution of AsyncTask", e);
                }

                if (registered) {
                    Log.d(TAG, "succesfully registered");
                    Intent intent = new Intent(this, ChatActivity.class);
                    intent.putExtra("username", username);
                    intent.putExtra("uuid", clientUUID);
                    startActivity(intent);
                } else{
                    // TODO get error code and say more about why it didn't work
                    Log.d(TAG, "something went wrong trying to register");
                    Toast.makeText(this, "Couldn't join the chat", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Username should not be empty", Toast.LENGTH_LONG).show();
            }

        }
    }

    public void updateJoinButton() {
        mButtonJoin.setText("Join Chat @ " + mChatServerIPString + ":" + mChatServerPORTString);
    }

    private class ServerConnectionTask extends AsyncTask<ConnectionParameters, Integer, Boolean> {

        private DatagramSocket socket = null;

        @Override
        protected Boolean doInBackground(ConnectionParameters... params){

            // TODO try register 5 times, use onProgressUpdate to notify how many tries we had

            String serverIPString = params[0].getServerIP();
            int serverPort = params[0].getServerPORT();
            String username = params[0].getUsername();
            String clientUUID = params[0].getClientUUID();

            InetAddress serverIP = null;
            try {
                serverIP = InetAddress.getByName(serverIPString);
            } catch (UnknownHostException e) {
                Log.e(TAG, "Hmmm... unknown host", e);
                return false;
            }
            try {
                socket = new DatagramSocket();
                socket.setSoTimeout(NetworkConsts.SOCKET_TIMEOUT);
            } catch (SocketException e) {
                Log.e(TAG, "Couldn't create UDP socket" ,e);
                return false;
            }

            // Exclusively send and receive to and from server
            socket.connect(serverIP, serverPort);

            MessageOut msgOut = new MessageOut(MessageTypes.REGISTER, username, clientUUID, null ,serverIP, serverPort);
            DatagramPacket packetOut = msgOut.getDatagramPacket();
            try {
                socket.send(packetOut);
            } catch (IOException e) {
                Log.e(TAG, "Couldn't send", e);
                return false;
            }
            Log.d(TAG, "Successfully send UDP packet");

            byte[] bufIn = new byte[NetworkConsts.PAYLOAD_SIZE];
            DatagramPacket packetIn = new DatagramPacket(bufIn, bufIn.length);
            try {
                socket.receive(packetIn);
            } catch (IOException e) {
                if (e instanceof SocketTimeoutException){
                    Log.e(TAG, "Socket timed out trying to receive", e);
                } else {
                    Log.e(TAG, "Couldn't receive", e);
                }
                // stop thread
                return false;
            }

            Log.d(TAG, "Successfully received UDP packet");
            MessageIn msgIn = new MessageIn(packetIn);
            if(msgIn.getType().equals(MessageTypes.ACK_MESSAGE)){
                return true;
            } else{
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            socket.close();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }
    }

    public class ConnectionParameters{

        private String mServerIP;
        private int mServerPORT;
        private String mClientUUID;
        private String mUsername;

        public ConnectionParameters(String serverIP, int serverPORT, String clientUUID, String username){
            mServerIP = serverIP;
            mServerPORT = serverPORT;
            mClientUUID = clientUUID;
            mUsername = username;
        }


        public String getServerIP() {
            return mServerIP;
        }

        public int getServerPORT() {
            return mServerPORT;
        }

        public String getClientUUID() {
            return mClientUUID;
        }

        public String getUsername() {
            return mUsername;
        }
    }

}
