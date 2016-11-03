package ch.ethz.inf.vs.a3.fabischn.chat;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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

import java.util.ArrayList;

import ch.ethz.inf.vs.a3.fabischn.message.ErrorCodes;
import ch.ethz.inf.vs.a3.fabischn.message.Message;
import ch.ethz.inf.vs.a3.fabischn.message.MessageIn;
import ch.ethz.inf.vs.a3.fabischn.queue.PriorityQueue;
import ch.ethz.inf.vs.a3.fabischn.udpclient.ConnectionParameters;
import ch.ethz.inf.vs.a3.fabischn.udpclient.DeregistrationResult;

public class ChatActivity extends AppCompatActivity implements Button.OnClickListener, DeregisterFragment.DeregisterCallbacks, FetchFragment.FetchCallbacks{

    private static final String TAG = ChatActivity.class.getSimpleName();
    private static final String TAG_DEREGISTERFRAGMENT = DeregisterFragment.class.getCanonicalName();
    private static final String TAG_FETCHFRAGMENT = FetchFragment.class.getCanonicalName();

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

    private FetchFragment mFetchFragment;
    private DeregisterFragment mDeregisterFragment;


    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Intent intent = getIntent();
        mClientUUID = intent.getStringExtra("uuid");
        mUsername = intent.getStringExtra("username");

        KEY_SETTING_IP = getString(R.string.key_setting_ip);
        KEY_SETTING_PORT = getString(R.string.key_setting_port);
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

        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        mDeregisterFragment = (DeregisterFragment) fragmentManager.findFragmentByTag(TAG_DEREGISTERFRAGMENT);
        mFetchFragment = (FetchFragment) fragmentManager.findFragmentByTag(TAG_FETCHFRAGMENT);

        // TODO check wifi connection
    }

    @Override
    protected void onResume() {
        // TODO test lifecycle, look at MainActivity
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Save UI state
        outState.putBoolean(getString(R.string.key_button_chatlog_enabled), mButtonChatlog.isEnabled());
        outState.putString(getString(R.string.key_button_chatlog_text), mButtonChatlog.getText().toString());
        outState.putBoolean(getString(R.string.key_text_chatlog_enabled), mTextChatlog.isEnabled());
        outState.putString(getString(R.string.key_text_chatlog_text), mTextChatlog.getText().toString());
        boolean visible = false;
        if (mLayoutProgressBar.getVisibility() == View.VISIBLE) {
            visible = true;
        }
        outState.putBoolean(getString(R.string.key_layout_chat_progress_visibility), visible);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore UI state
        mButtonChatlog.setEnabled(savedInstanceState.getBoolean(getString(R.string.key_button_chatlog_enabled)));
        mButtonChatlog.setText(savedInstanceState.getString(getString(R.string.key_button_chatlog_text)));
        mTextChatlog.setEnabled(savedInstanceState.getBoolean(getString(R.string.key_text_chatlog_enabled)));
        mTextChatlog.setText(savedInstanceState.getString(getString(R.string.key_text_chatlog_text)));
        boolean visible = savedInstanceState.getBoolean(getString(R.string.key_layout_chat_progress_visibility));
        if (visible) {
            mLayoutProgressBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_chatlog) {
            disableUIforFetch();
            removeFetchFragment();
            mFetchFragment = new FetchFragment();
            Bundle params = new Bundle();
            params.putSerializable(getString(R.string.key_connection_parameters), new ConnectionParameters(mServerIP, mServerPORT, mClientUUID, mUsername));
            mFetchFragment.setArguments(params);
            android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().add(mFetchFragment, TAG_FETCHFRAGMENT).commit();
        }
    }


    // The android back button on the bottom
    @Override
    public void onBackPressed() {
        deregister(BackActions.SYSTEM_BACK);
        // onPostExecute will call super.onBackPressed();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // from https://www.tutorialspoint.com/android/android_navigation.htm
        switch (item.getItemId()) {
            case android.R.id.home:
                deregister(BackActions.NAV_UP);
                return true;
        }
        return false;
    }

    public void disableUIforFetch() {
        mTextChatlog.setText("");
        mLayoutProgressBar.setVisibility(View.VISIBLE);
        mButtonChatlog.setText(getString(R.string.fetching_chat_log));
        mButtonChatlog.setEnabled(false);
    }

    public void readyUIforFetch() {
        mLayoutProgressBar.setVisibility(View.GONE);
        mButtonChatlog.setText(getString(R.string.get_chat_log));
        mButtonChatlog.setEnabled(true);
    }

    private void deregister(final BackActions backSource) {
        // from http://stackoverflow.com/questions/6413700/android-proper-way-to-use-onbackpressed
        new AlertDialog.Builder(this)
                .setTitle("Really Exit?")
                .setMessage("You will be unregistered from the chat ...")
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    // TODO put backsource into Bundle
                    // TODO freeze backbuttons until we get callback?

                    public void onClick(DialogInterface arg0, int arg1) {
//                        DeregisterFromServerTask deregisterTask = new DeregisterFromServerTask(ChatActivity.this, BackActions.NAV_UP);
//                        deregisterTask.execute(new ConnectionParameters(mServerIP, mServerPORT, mClientUUID, mUsername));
                        removeDeregisterFragment();
                        mDeregisterFragment = new DeregisterFragment();
                        Bundle params = new Bundle();
                        params.putSerializable(getString(R.string.key_connection_parameters), new ConnectionParameters(mServerIP, mServerPORT, mClientUUID, mUsername));
                        params.putSerializable(getString(R.string.key_back_source), backSource); // enums are inherently serializable
                        mDeregisterFragment.setArguments(params);
                        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
                        fragmentManager.beginTransaction().add(mDeregisterFragment, TAG_DEREGISTERFRAGMENT).commit();
                    }
                }).create().show();
    }

    public void removeDeregisterFragment() {

        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        if (mDeregisterFragment == null) {
            mDeregisterFragment = (DeregisterFragment) fragmentManager.findFragmentByTag(TAG_DEREGISTERFRAGMENT);
        }
        if (mDeregisterFragment != null) {
            mDeregisterFragment.cancelDeregisterTask();
            fragmentManager.beginTransaction().remove(mDeregisterFragment).commit();
            mDeregisterFragment = null;
        }
    }

    public void removeFetchFragment() {

        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        if (mFetchFragment == null) {
            mFetchFragment= (FetchFragment) fragmentManager.findFragmentByTag(TAG_FETCHFRAGMENT);
        }
        if (mFetchFragment!= null) {
            mFetchFragment.cancelFetchTask();
            fragmentManager.beginTransaction().remove(mFetchFragment).commit();
            mFetchFragment= null;
        }
    }

    @Override
    public void onProgressUpdateDeregister(int value) {

    }

    @Override
    public void onCancelledDeregister() {

    }

    @Override
    public void onPostExecuteDeregister(DeregistrationResult result) {
        if (result.getDeregisterStatus()) {
            // TODO kill any running fetchtask
            switch (result.getBackSource()) {
                case NAV_UP:
                    // TODO: MainActivity will be created and resumed
                    NavUtils.navigateUpFromSameTask(this);
                    break;
                case SYSTEM_BACK:
                    // TODO: MainActivity will be resumed
                    super.onBackPressed();
                    break;
                default:
                    Log.e(TAG, "Switch case default. Shouldn't be here, think harder!");
            }
        } else{
            Toast.makeText(ChatActivity.this, "Server responded: " + ErrorCodes.getStringError(result.getErrorCode()), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onProgressUpdateFetch(int value) {
    }

    @Override
    public void onCancelledFetch() {
    }

    @Override
    public void onPostExecuteFetch(PriorityQueue<Message> messageIns) {
        StringBuilder builder = new StringBuilder();

        if (messageIns != null && !messageIns.isEmpty()) {
            while (!messageIns.isEmpty()) {
                MessageIn msg = (MessageIn) messageIns.poll();
                builder.append(msg.getContent() + "\n");
            }
            builder.deleteCharAt(builder.length() - 1);
            mTextChatlog.setText(builder.toString());
            readyUIforFetch();
        } else if (messageIns == null) {
            // maybe make queue and errorcode a pair
            // TODO go back to mainactivity
        } else {
            mTextChatlog.setText("no messages on server");
            readyUIforFetch();
        }
    }


    public enum BackActions {
        NAV_UP, SYSTEM_BACK
    }


}
