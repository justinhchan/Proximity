package cmsc436.proximity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.Messages;
import com.google.android.gms.nearby.messages.PublishCallback;
import com.google.android.gms.nearby.messages.PublishOptions;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeCallback;
import com.google.android.gms.nearby.messages.SubscribeOptions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * An activity that allows a user to publish device information, and receive information about
 * nearby devices.
 * <p/>
 * The UI exposes a button to subscribe to broadcasts from nearby devices, and another button to
 * publish messages that can be read nearby subscribing devices. Both buttons toggle state,
 * allowing the user to cancel a subscription or stop publishing.
 * <p/>
 * This activity demonstrates the use of the
 * {@link Messages#subscribe(GoogleApiClient, MessageListener, SubscribeOptions)},
 * {@link Messages#unsubscribe(GoogleApiClient, MessageListener)},
 * {@link Messages#publish(GoogleApiClient, Message, PublishOptions)}, and
 * {@link Messages#unpublish(GoogleApiClient, Message)} for foreground publication and subscription.
 * <p/>a
 * We check the app's permissions and present an opt-in dialog to the user, who can then grant the
 * required location permission.
 * <p/>
 * Using Nearby for in the foreground is battery intensive, and pub-sub is best done for short
 * durations. In this sample, we set the TTL for publishing and subscribing to three minutes
 * using a {@link Strategy}. When the TTL is reached, a publication or subscription expires.
 */
public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "Proximity";

    private static final int TTL_IN_SECONDS = 2 * 60; // Two minutes.

    /**
     * Sets the time in seconds for a published message or a subscription to live. Set to two
     * minutes.
     */
    // i added the "earshot" part not sure what it does yet, maybe restricts to ultrasound
    private static final Strategy PUB_SUB_STRATEGY = new Strategy.Builder()
            .setTtlSeconds(TTL_IN_SECONDS).setDistanceType(Strategy.DISTANCE_TYPE_EARSHOT).build();



    /**
     * The entry point to Google Play Services.
     */
    private GoogleApiClient mGoogleApiClient;

    // Views.
    private Toolbar mToolbar;
    private SwitchCompat mPublishSwitch;
    private SwitchCompat mSubscribeSwitch;
    private AlertDialog mMessageDialog;
    private EditText mMessageText;
    private TextView currPubMessageDisplay;

    /**
     * The {@link Message} object used to broadcast information about the device to nearby devices.
     */
    private Message currentPubMessage;
    private String currentPubMessageString;

    /**
     * A {@link MessageListener} for processing messages from nearby devices.
     */
    private MessageListener mMessageListener;

    /**
     * Adapter for working with messages from nearby publishers.
     */
    private ArrayAdapter<String> mNearbyDevicesArrayAdapter;

    // internal storage
    private String currMessageStorageFileName = "currMessageFile";
    private String receivedMessagesStorageFileName = "allMessagesFile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar)findViewById(R.id.action_bar);
        setSupportActionBar(mToolbar);

        mSubscribeSwitch = (SwitchCompat) findViewById(R.id.subscribe_switch);
        mPublishSwitch = (SwitchCompat) findViewById(R.id.publish_switch);


        createMessageDialog();

        mMessageListener = new MessageListener() {
            @Override
            public void onFound(final Message message) {
                // Called when a new message is found.
                receiveMessage(new String(message.getContent()));
                Log.i(TAG, "received message " + new String(message.getContent()));
                Toast.makeText(getApplicationContext(), "Found message", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLost(final Message message) {
                // Called when a message is no longer detectable nearby.
                mNearbyDevicesArrayAdapter.remove("user");
                Toast.makeText(getApplicationContext(), "Message expired", Toast.LENGTH_SHORT).show();
            }
        };

        mSubscribeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // If GoogleApiClient is connected, perform sub actions in response to user action.
                // If it isn't connected, do nothing, and perform sub actions when it connects (see
                // onConnected()).
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    if (isChecked) {
                        subscribe();
                    } else {
                        unsubscribe();
                    }
                }
            }
        });

        mPublishSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // If GoogleApiClient is connected, perform pub actions in response to user action.
                // If it isn't connected, do nothing, and perform pub actions when it connects (see
                // onConnected()).
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    if (isChecked) {
                        publish();
                    } else {
                        unpublish();
                    }
                }
            }
        });

        Button fakeMsgBtn = findViewById(R.id.fakemsgbtn);
        fakeMsgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                receiveFakeMessage();
            }
        });

        final List<String> nearbyDevicesArrayList = new ArrayList<>();
        mNearbyDevicesArrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                nearbyDevicesArrayList);
        final ListView nearbyDevicesListView = (ListView) findViewById(
                R.id.nearby_devices_list_view);
        if (nearbyDevicesListView != null) {
            nearbyDevicesListView.setAdapter(mNearbyDevicesArrayAdapter);
        }
        buildGoogleApiClient();

        readFromStorage();
    }

    private void createMessageDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        final View dialog_view = inflater.inflate(R.layout.dialog_message, null);
        builder.setView(dialog_view);
        builder.setPositiveButton(R.string.dialog_message_save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                mMessageText   = (EditText) dialog_view.findViewById(R.id.dialog_message);
                currentPubMessageString = mMessageText.getText().toString();
                saveCurrentPubMessage();
                mMessageText.setText("");
                currentPubMessage = new Message(currentPubMessageString.getBytes());

                Toast.makeText(getApplicationContext(), "Sent message", Toast.LENGTH_LONG).show();
            }
        });
        builder.setNegativeButton(R.string.dialog_message_cancel, null);



        mMessageDialog = builder.create();

        currPubMessageDisplay = dialog_view.findViewById(R.id.dialog_current_message);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);

        return true;
    }

    // shows the Current Message dialog
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int res_id = item.getItemId();
        if (res_id == R.id.action_message) {

            if (currentPubMessageString != null && currPubMessageDisplay != null) {
                currPubMessageDisplay.setText(currentPubMessageString);
            }
            else {
                Log.i(TAG, "no message yet");
            }

            mMessageDialog.show();
        }
        return true;
    }



    private void receiveMessage(String newmsg) {
        mNearbyDevicesArrayAdapter.add( newmsg.toString());

    }

    // TODO this is for testing purposes
    private void receiveFakeMessage() {

        mMessageListener.onFound(new Message(("simulated message sent at " + System.currentTimeMillis()).getBytes()));
    }

    // read from storage to get a current message being published and the list of all received msgs
    // so they can be displayed
    private void readFromStorage() {
        // check if curr msg file already exists & if not create it
        if (!getFileStreamPath(currMessageStorageFileName).exists()) {
            try {
                getFileStreamPath(currMessageStorageFileName).createNewFile();
            } catch (IOException e) {
                Log.i(TAG, "IO exception creating curr msg file");
                e.printStackTrace();
            }
        }
        // read curr msg file
        try {
            FileInputStream fis = openFileInput(currMessageStorageFileName);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));

            String line;
            String sep = System.getProperty("line.separator");

            while (null != (line = br.readLine())) {
                currentPubMessageString = line;
            }

            br.close();

        } catch (IOException e) {
            Log.i(TAG, "IOException");
        }

        //TODO for now fake received messages are added anytime the file is opened here instead of when received
        // check if received msgs file already exists & if not create it
        if (!getFileStreamPath(receivedMessagesStorageFileName).exists()) {
            // read received msgs file
            try {
                FileInputStream fis = openFileInput(receivedMessagesStorageFileName);
                BufferedReader br = new BufferedReader(new InputStreamReader(fis));

                String line;
                String sep = System.getProperty("line.separator");

                while (null != (line = br.readLine())) {
                    mNearbyDevicesArrayAdapter.add(line);
                }

                br.close();

            } catch (IOException e) {
                Log.i(TAG, "IOException");
            }
        }
    }

    // store all received messages in internal storage
    private void storeReceivedMessages() {

        // check if received msgs file already exists & if not create it
        if (!getFileStreamPath(receivedMessagesStorageFileName).exists()) {
            try {
                getFileStreamPath(receivedMessagesStorageFileName).createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        try {
            FileOutputStream fos = openFileOutput(receivedMessagesStorageFileName, MODE_APPEND);

            PrintWriter pw = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(fos)));

            for (int i=0; i<mNearbyDevicesArrayAdapter.getCount(); i++) {
                pw.println(mNearbyDevicesArrayAdapter.getItem(i));
            }

            pw.close();
        } catch (FileNotFoundException e) {
            Log.i(TAG, "received messages file not found");
        }
    }

    // when the currently broadcasting message is changed, save it to internal storage
    private void saveCurrentPubMessage() {
        // check if curr msg file already exists & if not create it, write new str to file
        if (!getFileStreamPath(currMessageStorageFileName).exists()) {
            try {
                getFileStreamPath(currMessageStorageFileName).createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            FileOutputStream fos = openFileOutput(currMessageStorageFileName, MODE_PRIVATE);

            PrintWriter pw = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(fos)));

            pw.println(currentPubMessageString);

            pw.close();
        } catch (IOException e) {
            Log.i(TAG, "IO exception writing to curr msg file");
            e.printStackTrace();
        }
    }

    /**
     * Builds {@link GoogleApiClient}, enabling automatic lifecycle management using
     * {@link GoogleApiClient.Builder#enableAutoManage(FragmentActivity,
     * int, GoogleApiClient.OnConnectionFailedListener)}. I.e., GoogleApiClient connects in
     * {@link AppCompatActivity#onStart}, or if onStart() has already happened, it connects
     * immediately, and disconnects automatically in {@link AppCompatActivity#onStop}.
     */
    private void buildGoogleApiClient() {
        if (mGoogleApiClient != null) {
            return;
        }
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Nearby.MESSAGES_API)
                .addConnectionCallbacks(this)
                .enableAutoManage(this, this)
                .build();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        mPublishSwitch.setEnabled(false);
        mSubscribeSwitch.setEnabled(false);
        Log.i(TAG,"Exception while connecting to Google Play services: " +
                connectionResult.getErrorMessage());
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG,"Connection suspended. Error code: " + i);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "GoogleApiClient connected");
        // We use the Switch buttons in the UI to track whether we were previously doing pub/sub (
        // switch buttons retain state on orientation change). Since the GoogleApiClient disconnects
        // when the activity is destroyed, foreground pubs/subs do not survive device rotation. Once
        // this activity is re-created and GoogleApiClient connects, we check the UI and pub/sub
        // again if necessary.
        if (mPublishSwitch.isChecked()) {
            publish();
        }
        if (mSubscribeSwitch.isChecked()) {
            subscribe();
        }
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "onStop()");
        super.onStop();
        storeReceivedMessages();
        saveCurrentPubMessage();

        if (mPublishSwitch.isChecked())
            unpublish();

        if (mSubscribeSwitch.isChecked())
            unsubscribe();
    }

    /**
     * Subscribes to messages from nearby devices and updates the UI if the subscription either
     * fails or TTLs.
     */
    private void subscribe() {
        Log.i(TAG, "Subscribing");
        mNearbyDevicesArrayAdapter.clear();
        SubscribeOptions options = new SubscribeOptions.Builder()
                .setStrategy(PUB_SUB_STRATEGY)
                .setCallback(new SubscribeCallback() {
                    @Override
                    public void onExpired() {
                        super.onExpired();
                        Log.i(TAG, "No longer subscribing");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mSubscribeSwitch.setChecked(false);
                            }
                        });
                    }
                }).build();

        Nearby.Messages.subscribe(mGoogleApiClient, mMessageListener, options)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Subscribed successfully.");
                        } else {
                            mSubscribeSwitch.setChecked(false);
                        }
                    }
                });
    }

    /**
     * Publishes a message to nearby devices and updates the UI if the publication either fails or
     * TTLs.
     */
    private void publish() {
        Log.i(TAG, "Publishing");
        PublishOptions options = new PublishOptions.Builder()
                .setStrategy(PUB_SUB_STRATEGY)
                .setCallback(new PublishCallback() {
                    @Override
                    public void onExpired() {
                        super.onExpired();
                        Log.i(TAG, "No longer publishing");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mPublishSwitch.setChecked(false);
                            }
                        });
                    }
                }).build();

        if (currentPubMessage == null) {
            Log.i(TAG, "message was null");
            //currentPubMessageString = "No message is being published yet";
            currentPubMessage = new Message(currentPubMessageString.getBytes());
        }
        else {
        }
        Nearby.Messages.publish(mGoogleApiClient, currentPubMessage, options)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Published successfully.");
                        } else {
                            Log.i(TAG, status.getStatus().toString());
                            Log.i(TAG, status.getStatusMessage());
                            Log.i(TAG, status.getStatusCode() + "");
                            mPublishSwitch.setChecked(false);
                        }
                    }
                });
    }

    /**
     * Stops subscribing to messages from nearby devices.
     */
    private void unsubscribe() {
        Log.i(TAG, "Unsubscribing.");
        Nearby.Messages.unsubscribe(mGoogleApiClient, mMessageListener);
    }

    /**
     * Stops publishing message to nearby devices.
     */
    private void unpublish() {
        Log.i(TAG, "Unpublishing.");
        Nearby.Messages.unpublish(mGoogleApiClient, currentPubMessage);
    }

}