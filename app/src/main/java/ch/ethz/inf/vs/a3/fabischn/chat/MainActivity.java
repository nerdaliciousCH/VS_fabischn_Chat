package ch.ethz.inf.vs.a3.fabischn.chat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener, Button.OnClickListener{


    private static final String TAG = MainActivity.class.getSimpleName();

    private static String KEY_SETTING_IP;
    private static String KEY_SETTING_PORT;

    private InetAddress mChatServerIP;
    private int mChatServerPORT;

    private EditText mEditTextUsername;
    private Button mButtonJoin;

    private SharedPreferences mSharedPreferences;

    DatagramSocket mUDPSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        KEY_SETTING_IP = getResources().getString(R.string.setting_ip);
        KEY_SETTING_PORT = getResources().getString(R.string.setting_port);

        mEditTextUsername = (EditText) findViewById(R.id.edittext_username);
        mButtonJoin = (Button) findViewById(R.id.btn_join);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        //TODO
        try {
            mChatServerIP = InetAddress.getByName(mSharedPreferences.getString(KEY_SETTING_IP, "no ip"));
        } catch (UnknownHostException e) {
            Log.e(TAG, "Malformed IP in preferences", e);
        }
        mChatServerPORT = mSharedPreferences.getInt(KEY_SETTING_PORT, -1);

        try {
            mUDPSocket = new DatagramSocket();
        } catch (SocketException e) {
            Log.e(TAG, "Exploded trying to instantiate DatagramSocket", e);
        }
        if (mUDPSocket != null){

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    protected void onPause() {
        super.onPause();
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
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
        if (key == KEY_SETTING_IP){
            Log.d(TAG, sharedPreferences.getString(KEY_SETTING_IP, "no preference found"));
        }

        if (key == KEY_SETTING_PORT){
            Log.d(TAG, sharedPreferences.getString(KEY_SETTING_PORT, "no preference found"));
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_join){
            Intent intent = new Intent(this, ChatActivity.class);
            startActivity(intent);
        }
    }
}
