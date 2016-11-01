package ch.ethz.inf.vs.a3.fabischn.chat;

import android.app.Activity;
import android.content.Context;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Comparator;

import ch.ethz.inf.vs.a3.fabischn.message.ErrorCodes;
import ch.ethz.inf.vs.a3.fabischn.message.Message;
import ch.ethz.inf.vs.a3.fabischn.message.MessageComparator;
import ch.ethz.inf.vs.a3.fabischn.message.MessageIn;
import ch.ethz.inf.vs.a3.fabischn.message.MessageOut;
import ch.ethz.inf.vs.a3.fabischn.message.MessageTypes;
import ch.ethz.inf.vs.a3.fabischn.queue.PriorityQueue;
import ch.ethz.inf.vs.a3.fabischn.udpclient.ConnectionParameters;
import ch.ethz.inf.vs.a3.fabischn.udpclient.ConnectionResult;
import ch.ethz.inf.vs.a3.fabischn.udpclient.NetworkConsts;

public class ChatActivity extends AppCompatActivity implements Button.OnClickListener {

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

    private RelativeLayout mLayoutProgressBar;


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
        mLayoutProgressBar = (RelativeLayout) findViewById(R.id.layout_chat_loading_bar);
        mLayoutProgressBar.setVisibility(View.GONE);


        mTextUsername.setText(mUsername);
        mTextServer.setText(mServerIP + ":" + mServerPORT);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResume() {
        // TODO test lifecycle, look at MainActivity
        super.onResume();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_chatlog) {
            disableUIforFetch();
            FetchChatlogTask fetchChatlogTask = new FetchChatlogTask();
            fetchChatlogTask.execute(new ConnectionParameters(mServerIP, mServerPORT, mClientUUID, mUsername));
        }
    }


    // The android back button on the bottom
    @Override
    public void onBackPressed() {
        deregister();
        // onPostExecute will call super.onBackPressed();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // from https://www.tutorialspoint.com/android/android_navigation.htm
        switch (item.getItemId()) {
            case android.R.id.home:
                deregister();
                return true;
        }
        return false;
    }

    public void disableUIforFetch(){
        mTextChatlog.setText("");
        mLayoutProgressBar.setVisibility(View.VISIBLE);
        mButtonChatlog.setText(getString(R.string.fetching_chat_log));
        mButtonChatlog.setEnabled(false);
    }

    public void readyUIforFetch(){
        mLayoutProgressBar.setVisibility(View.GONE);
        mButtonChatlog.setText(getString(R.string.get_chat_log));
        mButtonChatlog.setEnabled(true);
    }

    private void deregister() {
        // from http://stackoverflow.com/questions/6413700/android-proper-way-to-use-onbackpressed
        new AlertDialog.Builder(this)
                .setTitle("Really Exit?")
                .setMessage("Are you sure you want to exit?")
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface arg0, int arg1) {
                        DeregisterFromServerTask deregisterTask = new DeregisterFromServerTask(ChatActivity.this, BackActions.NAV_UP);
                        deregisterTask.execute(new ConnectionParameters(mServerIP, mServerPORT, mClientUUID, mUsername));
                    }
                }).create().show();
    }

    private class FetchChatlogTask extends AsyncTask<ConnectionParameters, Integer, PriorityQueue<Message>> {

        private DatagramSocket socket = null;

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
                Log.e(TAG, "Hmmm... unknown host", e);
                return null;
            }
            try {
                socket = new DatagramSocket();
                socket.setSoTimeout(NetworkConsts.SOCKET_TIMEOUT);
            } catch (SocketException e) {
                Log.e(TAG, "Couldn't create UDP socket", e);
                return null;
            }

            // Exclusively send and receive to and from server
            socket.connect(serverIP, serverPort);

            MessageOut msgOut = new MessageOut(MessageTypes.RETRIEVE_CHAT_LOG, username, clientUUID, null, serverIP, serverPort);
            DatagramPacket packetOut = msgOut.getDatagramPacket();
            try {
                socket.send(packetOut);
            } catch (IOException e) {
                Log.e(TAG, "Couldn't send", e);
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
                    } else {
                        Log.e(TAG, "Couldn't receive", e);
                        return null;
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(PriorityQueue<Message> messageIns) {
            StringBuilder builder = new StringBuilder();

            if (messageIns != null && !messageIns.isEmpty()) {
                while (!messageIns.isEmpty()) {
                    MessageIn msg = (MessageIn) messageIns.poll();
                    builder.append(msg.getContent() + "\n");
                }
                builder.deleteCharAt(builder.length() - 1);
                mTextChatlog.setText(builder.toString());
            }

            // Reset UI
            mTextChatlog.setText(builder.toString());
            readyUIforFetch();
        }

    }

    private class DeregisterFromServerTask extends AsyncTask<ConnectionParameters, Void, ConnectionResult> {

        private Context context;
        private DatagramSocket socket;
        private BackActions source;

        public DeregisterFromServerTask(Context context, BackActions source) {
            this.context = context;
            this.source = source;
        }

        @Override
        protected ConnectionResult doInBackground(ConnectionParameters... params) {
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
            MessageOut msgOut = new MessageOut(MessageTypes.DEREGISTER, username, clientUUID, null, serverIP, serverPort);
            DatagramPacket packetOut = msgOut.getDatagramPacket();

            // variables for retries & packet buffer
            byte[] bufIn;
            DatagramPacket packetIn = null;

            try {
                socket.send(packetOut);
            } catch (IOException e) {
                Log.e(TAG, "Couldn't send", e);
                return new ConnectionResult(false, ConnectionResult.NO_ERROR);
            }

            // create input buffer, after knowing send successful
            bufIn = new byte[NetworkConsts.PAYLOAD_SIZE];
            packetIn = new DatagramPacket(bufIn, bufIn.length);

            try {
                socket.receive(packetIn);
            } catch (IOException e) {
                if (e instanceof SocketTimeoutException) {
                    Log.d(TAG, "Socket TIMEOUT");
                } else {
                    Log.e(TAG, "Couldn't receive", e);
                    return new ConnectionResult(false, ConnectionResult.NO_ERROR);
                }
            }

            MessageIn msgIn = new MessageIn(packetIn);
            if (msgIn.getType().equals(MessageTypes.ACK_MESSAGE)) {
                return new ConnectionResult(true, ConnectionResult.NO_ERROR);
            } else {
                Toast.makeText(ChatActivity.this, ErrorCodes.getStringError(Integer.parseInt(msgIn.getMessage())), Toast.LENGTH_LONG).show();
                return new ConnectionResult(false, ConnectionResult.NO_ERROR);
            }
        }

        @Override
        protected void onPostExecute(ConnectionResult connectionResult) {
            if (connectionResult.getRegisterStatus()) {
                switch (source) {
                    case NAV_UP:
                        // TODO: MainActivity will be created and resumed
                        NavUtils.navigateUpFromSameTask((Activity) context);
                        break;
                    case SYSTEM_BACK:
                        // TODO: MainActivity will be resumed
                        ChatActivity.super.onBackPressed();
                        break;
                    default:
                        Log.e(TAG, "Switch case default. Shouldn't be here, think harder!");
                }
            }
        }
    }

    public enum BackActions {
        NAV_UP, SYSTEM_BACK
    }


}
