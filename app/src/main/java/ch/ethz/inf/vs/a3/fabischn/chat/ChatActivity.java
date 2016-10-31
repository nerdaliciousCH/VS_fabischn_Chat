package ch.ethz.inf.vs.a3.fabischn.chat;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import ch.ethz.inf.vs.a3.fabischn.udpclient.UDPClient;

public class ChatActivity extends AppCompatActivity {

    private String mServerIP;
    private int mServerPORT;
    private String mClientUUID;
    private String mUsername;

    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Intent intent = getIntent();

        mClientUUID = intent.getStringExtra("uuid");
        mUsername = intent.getStringExtra("username");

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mServerIP = mSharedPreferences.getString(getResources().getString(R.string.setting_ip), "no ip");
        mServerPORT = Integer.parseInt(mSharedPreferences.getString(getResources().getString(R.string.setting_port), "no port"));

        // TODO use handler?
        UDPClient udpClient = new UDPClient(mUsername, mServerIP, mServerPORT, null);

    }


    public static class UDPClientHandler extends Handler {

        public final static String TAG = UDPClientHandler.class.getSimpleName();

        private Activity mParent;

        public UDPClientHandler(Activity parent) {
            mParent = parent;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.d(TAG, "android.os.Handler handled an android.os.Message");
        }
    }
}
