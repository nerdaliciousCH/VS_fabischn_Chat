package ch.ethz.inf.vs.a3.fabischn.chat;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import ch.ethz.inf.vs.a3.fabischn.message.MessageIn;
import ch.ethz.inf.vs.a3.fabischn.message.MessageOut;
import ch.ethz.inf.vs.a3.fabischn.message.MessageTypes;
import ch.ethz.inf.vs.a3.fabischn.udpclient.ConnectionParameters;
import ch.ethz.inf.vs.a3.fabischn.udpclient.NetworkConsts;

public class ChatActivity extends AppCompatActivity implements Button.OnClickListener{

    private static final String TAG = ChatActivity.class.getSimpleName();

    private static String KEY_SETTING_IP;
    private static String KEY_SETTING_PORT;

    private String mServerIP;
    private int mServerPORT;
    private String mClientUUID;
    private String mUsername;

    private ArrayList<MessageIn> mMessages;

    private TextView mTextUsername;
    private TextView mTextServer;
    private TextView mTextChatlog;

    private Button mButtonChatlog;

    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Intent intent = getIntent();
        mClientUUID = intent.getStringExtra("uuid");
        mUsername = intent.getStringExtra("username");

        KEY_SETTING_IP = getString(R.string.setting_ip);
        KEY_SETTING_PORT = getString(R.string.setting_port);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mServerIP = mSharedPreferences.getString(KEY_SETTING_IP, "no ip");
        mServerPORT = Integer.parseInt(mSharedPreferences.getString(KEY_SETTING_PORT, "no port"));

        mTextUsername = (TextView) findViewById(R.id.text_username);
        mTextServer = (TextView) findViewById(R.id.text_server);
        mTextChatlog = (TextView) findViewById(R.id.text_chatlog);
        mButtonChatlog = (Button) findViewById(R.id.btn_chatlog);
        // FIXME maybe onResume?
        mTextUsername.setText(mUsername);
        mTextServer.setText(mServerIP + ":" + mServerPORT);

        Log.d(TAG, "IP: " + mServerIP + "\n" + "Port: " + mServerPORT + "\n" + "username: " + mUsername + "\n" + "UUID: " + mClientUUID + "\n");

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_chatlog){
            mButtonChatlog.setText("Fetching chat log ...");
            mButtonChatlog.setEnabled(false);
            FetchChatlogTask fetchChatlogTask = new FetchChatlogTask();
            fetchChatlogTask.execute(new ConnectionParameters(mServerIP, mServerPORT, mClientUUID, mUsername));
        }
    }

    // The android back button on the bottom
    @Override
    public void onBackPressed() {
        // from http://stackoverflow.com/questions/6413700/android-proper-way-to-use-onbackpressed
        new AlertDialog.Builder(this)
                .setTitle("Really Exit?")
                .setMessage("Are you sure you want to exit?")
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface arg0, int arg1) {
                        // TODO deregister from server
                        ChatActivity.super.onBackPressed();
                    }
                }).create().show();
    }

    // from https://www.tutorialspoint.com/android/android_navigation.htm


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
//                new AlertDialog.Builder(this)
//                        .setTitle("Really Exit?")
//                        .setMessage("Are you sure you want to exit?")
//                        .setNegativeButton(android.R.string.no, null)
//                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
//
//                            public void onClick(DialogInterface arg0, int arg1) {
//                                // TODO deregister from server
//                                ChatActivity.super.onBackPressed();
//                            }
//                        }).create().show();
                // TODO deregister from server
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return false;
    }

    private class FetchChatlogTask extends AsyncTask<ConnectionParameters, Integer, ArrayList<MessageIn>> {

        private DatagramSocket socket = null;

        @Override
        protected ArrayList<MessageIn> doInBackground(ConnectionParameters... params){

            String serverIPString = params[0].getServerIP();
            int serverPort = params[0].getServerPORT();
            String username = params[0].getUsername();
            String clientUUID = params[0].getClientUUID();

            InetAddress serverIP = null;
            try {
                serverIP = InetAddress.getByName(serverIPString);
            } catch (UnknownHostException e) {
                Log.e(TAG, "Hmmm... unknown host", e);
                return null;
            }
            try {
                socket = new DatagramSocket();
                socket.setSoTimeout(NetworkConsts.SOCKET_TIMEOUT);
            } catch (SocketException e) {
                Log.e(TAG, "Couldn't create UDP socket" ,e);
                return null;
            }

            // Exclusively send and receive to and from server
            socket.connect(serverIP, serverPort);

            MessageOut msgOut = new MessageOut(MessageTypes.RETRIEVE_CHAT_LOG, username, clientUUID, null ,serverIP, serverPort);
            DatagramPacket packetOut = msgOut.getDatagramPacket();
            try {
                socket.send(packetOut);
            } catch (IOException e) {
                Log.e(TAG, "Couldn't send", e);
                return null;
            }
            Log.d(TAG, "Successfully send UDP packet");

            ArrayList<MessageIn> messages = new ArrayList<>();

            byte[] bufIn = null;
            DatagramPacket packetIn = null;

            int count = 0;

            while(true) {
                bufIn = new byte[NetworkConsts.PAYLOAD_SIZE];
                packetIn = new DatagramPacket(bufIn, bufIn.length);
                try {
                    socket.receive(packetIn);
                    Log.d(TAG, "Successfully received UDP packet");
                    MessageIn msg = new MessageIn(packetIn);
                    Log.d(TAG, "Got Message:" + msg.getMessage());
                    messages.add(msg);
                    publishProgress(new Integer(count++));
                } catch (IOException e) {
                    if (e instanceof SocketTimeoutException) {
                        Log.e(TAG, "Socket timed out trying to receive", e);
                        return messages;
                    } else {
                        Log.e(TAG, "Couldn't receive", e);
                        return null;
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(ArrayList<MessageIn> messageIns) {
            StringBuilder builder = new StringBuilder();

            // TODO order messages, using priority queue and messagecomparator
            if (messageIns != null && !messageIns.isEmpty()){
                for (MessageIn msg : messageIns){
                    Log.d(TAG, "Message: " + msg.getMessage());
                    builder.append(msg.getMessage() + "\n");
                }
            }
            // TODO check scrollable


            mTextChatlog.setText(builder.toString());
            // TODO values/strings
            mButtonChatlog.setText("got the log");
            mButtonChatlog.setEnabled(true);

        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            // maybe runonuithread?
//            mButtonChatlog.setText(values[0]);
        }
    }

//    private class DeregisterFromServerTask extends AsyncTask<ConnectionParameters, Void, Void>{
//
//    }

}
