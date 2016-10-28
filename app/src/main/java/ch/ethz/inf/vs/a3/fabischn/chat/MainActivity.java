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

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener, Button.OnClickListener{


    private static final String TAG = MainActivity.class.getSimpleName();

    private static String KEY_SETTING_IP;
    private static String KEY_SETTING_PORT;

    EditText EditTextUsername;
    Button ButtonJoin;

    DatagramSocket udpSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        KEY_SETTING_IP = getResources().getString(R.string.setting_ip);
        KEY_SETTING_PORT = getResources().getString(R.string.setting_port);

        EditTextUsername = (EditText) findViewById(R.id.edittext_username);
        ButtonJoin = (Button) findViewById(R.id.btn_join);

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

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
            Log.d(TAG, "IP changed");
            Log.d(TAG, sharedPreferences.getString(KEY_SETTING_IP, "no preference found"));
        }

        if (key == KEY_SETTING_PORT){
            Log.d(TAG, "PORT changed");
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
