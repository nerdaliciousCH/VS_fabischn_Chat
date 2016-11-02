package ch.ethz.inf.vs.a3.fabischn.chat;

import android.app.FragmentManager;
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
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.UUID;

import ch.ethz.inf.vs.a3.fabischn.message.ErrorCodes;
import ch.ethz.inf.vs.a3.fabischn.udpclient.ConnectionParameters;
import ch.ethz.inf.vs.a3.fabischn.udpclient.ConnectionResult;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener, Button.OnClickListener, MainFragment.TaskCallbacks {


    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String TAG_MAINFRAGMENT = MainFragment.class.getSimpleName();

    private static String KEY_SETTING_IP;
    private static String KEY_SETTING_PORT;

    private String mServerIP;
    private int mServerPORT;

    private String mClientUUID;
    private String mUsername;


    private EditText mEditTextUsername;
    private Button mButtonJoin;
    private RelativeLayout mLayoutProgressBar;

    private MainFragment mMainFragment;

    private SharedPreferences mSharedPreferences;
//    private ServerConnectionTask mConnectionTask;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        KEY_SETTING_IP = getString(R.string.key_setting_ip);
        KEY_SETTING_PORT = getString(R.string.key_setting_port);

        mEditTextUsername = (EditText) findViewById(R.id.edittext_username);
        mButtonJoin = (Button) findViewById(R.id.btn_join);
        mLayoutProgressBar = (RelativeLayout) findViewById(R.id.layout_main_loading_bar);
        mLayoutProgressBar.setVisibility(View.GONE);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        mServerIP = getSettingsIP();
        mServerPORT = getSettingsPORT();
        updateJoinButtonServerAddress();
        enableUI();

        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        mMainFragment = (MainFragment) fragmentManager.findFragmentByTag(TAG_MAINFRAGMENT);

        // TODO check wifi connection
        // TODO where does usernametext go when we come back from settings activity?
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(getString(R.string.key_button_join_enabled), mButtonJoin.isEnabled());
        outState.putString(getString(R.string.key_button_join_text), mButtonJoin.getText().toString());
        outState.putBoolean(getString(R.string.key_text_username_enabled), mEditTextUsername.isEnabled());
        boolean visible = false;
        if (mLayoutProgressBar.getVisibility() == View.VISIBLE) {
            visible = true;
        }
        outState.putBoolean(getString(R.string.key_layout_progress_visibility), visible);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mEditTextUsername.setEnabled(savedInstanceState.getBoolean(getString(R.string.key_text_username_enabled)));
        mButtonJoin.setEnabled(savedInstanceState.getBoolean(getString(R.string.key_button_join_enabled)));
        mButtonJoin.setText(savedInstanceState.getString(getString(R.string.key_button_join_text)));
        boolean visible = savedInstanceState.getBoolean(getString(R.string.key_layout_progress_visibility));
        if (visible) {
            mLayoutProgressBar.setVisibility(View.VISIBLE);
        }
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

                mClientUUID = UUID.randomUUID().toString();
                mButtonJoin.setText(getString(R.string.trying_connect));
                disableUI();
                removeFragment();
                mMainFragment = new MainFragment();
                Bundle connParams = new Bundle();
                connParams.putSerializable(getString(R.string.key_connection_parameters), new ConnectionParameters(mServerIP, mServerPORT, mClientUUID, mUsername));
                mMainFragment.setArguments(connParams);
                android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction().add(mMainFragment, TAG_MAINFRAGMENT).commit();

            } else {
                Toast.makeText(this, "Invalid username: Must be non empty and not contain spaces, tabs or newlines", Toast.LENGTH_SHORT).show();
            }

        }
    }

    public void updateJoinButtonServerAddress() {
        mButtonJoin.setText("Join Chat @ " + mServerIP + ":" + mServerPORT);
    }

    public void disableUI() {
        mLayoutProgressBar.setVisibility(View.VISIBLE);
        mButtonJoin.setEnabled(false);
        mEditTextUsername.setEnabled(false);
    }

    public void enableUI() {
        mLayoutProgressBar.setVisibility(View.GONE);
        mButtonJoin.setEnabled(true);
        mEditTextUsername.setEnabled(true);
    }

    public void removeFragment() {

        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        if (mMainFragment == null) {
            mMainFragment = (MainFragment) fragmentManager.findFragmentByTag(TAG_MAINFRAGMENT);
        }
        if (mMainFragment != null) {
            mMainFragment.cancelRegisterTask();
            fragmentManager.beginTransaction().remove(mMainFragment).commit();
            mMainFragment = null;
        }

    }


    public String getSettingsIP() {
        return mSharedPreferences.getString(KEY_SETTING_IP, "no ip");
    }

    public int getSettingsPORT() {
        return Integer.parseInt(mSharedPreferences.getString(KEY_SETTING_PORT, "0"));
    }

    @Override
    public void onProgressUpdate(int value) {
        switch (value) {
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

    @Override
    public void onCancelled() {
        Toast.makeText(this, "Registration was aborted", Toast.LENGTH_SHORT).show();
        mMainFragment = null;
        updateJoinButtonServerAddress();
        enableUI();
    }

    @Override
    public void onPostExecute(ConnectionResult result) {
        Log.d(TAG, "onPostExecute");
        if (result.getRegisterStatus()) {
            Log.d(TAG, "success register");
            removeFragment();
            Toast.makeText(this, "Registration succesfull!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("username", mUsername);
            intent.putExtra("uuid", mClientUUID);
            startActivity(intent);
        } else {
            int errorCode = result.getErrorCode();
            String errorMessage = "Unknown error";
            if (errorCode >= 0 && errorCode < 7) {
                // the protocol error codes and my own errorcodes
                errorMessage = ErrorCodes.getStringError(errorCode);
            }
            Toast.makeText(this, "Couldn't join the chat: " + errorMessage, Toast.LENGTH_SHORT).show();
            updateJoinButtonServerAddress();
            enableUI();
        }
    }
}
