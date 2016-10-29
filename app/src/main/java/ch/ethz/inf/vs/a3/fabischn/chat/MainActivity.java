package ch.ethz.inf.vs.a3.fabischn.chat;

import android.content.Intent;
import android.content.SharedPreferences;
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
            // TODO try connecting and joining chat. If ok, transition to Chat activity
            String username = mEditTextUsername.getText().toString();
            if (!username.equals("")) {
                UDPClient udpClient = new UDPClient(username, mChatServerIPString, mChatServerPORTInteger, 4);
                udpClient.start();
            } else {
                Toast.makeText(this, "Username should not be empty", Toast.LENGTH_LONG).show();
            }
//            Intent intent = new Intent(this, ChatActivity.class);
//            startActivity(intent);
        }
    }

    public void updateJoinButton() {
        mButtonJoin.setText("Join Chat @ " + mChatServerIPString + ":" + mChatServerPORTString);
    }
}
