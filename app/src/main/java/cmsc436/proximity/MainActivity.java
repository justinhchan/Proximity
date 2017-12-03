package cmsc436.proximity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
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

import org.w3c.dom.Text;

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

    private static final int TTL_IN_SECONDS = 15; // 15 seconds

    /**
     * Sets the time in seconds for a published message or a subscription to live. Set to two
     * minutes.
     */
    private static final Strategy PUB_SUB_STRATEGY = new Strategy.Builder()
            .setTtlSeconds(TTL_IN_SECONDS).build();



    /**
     * The entry point to Google Play Services.
     */
    private GoogleApiClient mGoogleApiClient;

    // Views.
    private Toolbar mToolbar;
//    private SwitchCompat mPublishSwitch;
//    private SwitchCompat mSubscribeSwitch;
//    private AlertDialog mMessageDialog;
//    private EditText mMessageText;
//    private TextView currPubMessageDisplay;

    /**
     * The {@link Message} object used to broadcast information about the device to nearby devices.
     */
//    private Message currentPubMessage;
//    private String currentPubMessageString;

    /**
     * A {@link MessageListener} for processing messages from nearby devices.
     */
    private MessageListener mMessageListener;

    /**
     * Adapter for working with messages from nearby publishers.
     */
    private ArrayAdapter<String> mNearbyDevicesArrayAdapter;

    // internal storage
//    private String currMessageStorageFileName = "currMessageFile";
//    private String receivedMessagesStorageFileName = "allMessagesFile";

    //variables for guess who game
    private String thisPlayerName = "";
    private Message currentPublishingMessage;
    private ArrayList<String> otherPlayers;
    private AlertDialog mProfileDialog;
    private Boolean isGameRunner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar)findViewById(R.id.action_bar);
        setSupportActionBar(mToolbar);

//        mSubscribeSwitch = (SwitchCompat) findViewById(R.id.subscribe_switch);
//        mPublishSwitch = (SwitchCompat) findViewById(R.id.publish_switch);

        otherPlayers = new ArrayList<String>();
        isGameRunner = false;
//        createMessageDialog();

        mMessageListener = new MessageListener() {
            @Override
            public void onFound(final Message message) {
                // Called when a new message is found.
                String messageContent = new String(message.getContent());
                if (messageContent.startsWith("myname: ")) {
                    Log.i(TAG, "message from " + new String(message.getContent()));
                    String otherPlayerName = messageContent.split(" ")[1];
                    otherPlayers.add(otherPlayerName);
                    // Using otherPlayerName to populate list of messages for testing purposes
                    // In the finished app, we will be using actual messages to populate the list
                    // instead of using playerNames
                    mNearbyDevicesArrayAdapter.add(otherPlayerName);
                    Toast.makeText(getApplicationContext(), "Found " + otherPlayerName, Toast.LENGTH_SHORT).show();
                }
                else if (messageContent.startsWith("gamerunner")) {
                    String gameRunner = messageContent.split(" ")[1];
                    Log.i(TAG, gameRunner + " is the game runner");
                    if (isGameRunner) {
                        Log.i(TAG, "game runner conflict");
                        // we have a problem
                    }
                }
                Log.i(TAG, "received message " + new String(message.getContent()));
            }

            @Override
            public void onLost(final Message message) {
                // Called when a message is no longer detectable nearby.
                mNearbyDevicesArrayAdapter.remove("user");
                Toast.makeText(getApplicationContext(), "Message expired", Toast.LENGTH_SHORT).show();
            }
        };

//        mSubscribeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                // If GoogleApiClient is connected, perform sub actions in response to user action.
//                // If it isn't connected, do nothing, and perform sub actions when it connects (see
//                // onConnected()).
//                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
//                    if (isChecked) {
//                        subscribe();
//                    } else {
//                        unsubscribe();
//                    }
//                }
//            }
//        });

//        mPublishSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                // If GoogleApiClient is connected, perform pub actions in response to user action.
//                // If it isn't connected, do nothing, and perform pub actions when it connects (see
//                // onConnected()).
//                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
//                    if (isChecked) {
//                        publish();
//                    } else {
//                        unpublish();
//                    }
//                }
//            }
//        });

        Button startGameBtn = findViewById(R.id.startgamebtn);
        startGameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { startGame(); }
        });

        Button fakeMsgBtn = findViewById(R.id.fakemsgbtn);
        fakeMsgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                receiveFakeMessage();
            }
        });

        final ArrayList<String> nearbyDevicesArrayList = new ArrayList<>();
        mNearbyDevicesArrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                nearbyDevicesArrayList);
        ListView nearbyDevicesListView = (ListView) findViewById(
                R.id.nearby_devices_list_view);

        // Using ListActivity's setEmptyView method automatically
        // checks if the adapter is empty or not. If the adapter is
        // empty, then is displays "No messages received." Otherwise,
        // it populates the ListView with messages from the array.
        TextView emptyText = (TextView) findViewById(R.id.emptyT);
        nearbyDevicesListView.setEmptyView(emptyText);
        nearbyDevicesListView.setAdapter(mNearbyDevicesArrayAdapter);

        // When a message in the ListView is selected, the user is sent to
        // a page that displays the message and a list of possible users who
        // could've sent that message.
        // TODO: When a user rapidly clicks on the message, multiple activites are
        // created
        nearbyDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(MainActivity.this, ChooseMessageActivity.class);
                Bundle mBundle = new Bundle();
                // Retrieve otherPlayers list to send into the new activity
                mBundle.putStringArrayList("otherPlayers", otherPlayers);
                // Player who sent the message
                // mBundle.putString("gameRunner", gameRunner);
                // Message that was selected
                // mBundle.putString("message", message);
                intent.putExtras(mBundle);
                // Should be startActivity for result which indicates
                // a score based on correct guess
                startActivity(intent);
            }
        });
        buildGoogleApiClient();

//        readFromStorage();
    }

    /* Creates action bar menu */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);

        return true;
    }

    // open a prompt asking for user's name
    // start publishing the user's name to nearby devices
    // start subscribing for other players' names from nearby devices
    // after 30 seconds stop subscribing and publishing
    private void startGame() {
//        openNamePrompt();
    }

//    private void openNamePrompt() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//
//        // Get the layout inflater
//        LayoutInflater inflater = this.getLayoutInflater();
//
//        // Inflate and set the layout for the dialog
//        // Pass null as the parent view because its going in the dialog layout
//        final View dialog_view = inflater.inflate(R.layout.nameprompt, null);
//        builder.setView(dialog_view);
//        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int id) {
//                EditText nameEntry   = (EditText) dialog_view.findViewById(R.id.dialog_name);
//                thisPlayerName = nameEntry.getText().toString();
//                nameEntry.setText("");
//
//                startPublishingName();
//                startFindingOtherPlayers();
//            }
//        });
//
//        profileDialog = builder.create();
//        profileDialog.show();
//    }

    // publish this player's name to nearby devices for 30 secs
    private void startPublishingName() {
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
                                    Toast.makeText(getApplicationContext(), "published name", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }).build();

            // publish a message "myname: <name>". other devices will extract the name from the message and add to their list
            currentPublishingMessage = new Message(("myname: " + thisPlayerName).getBytes());
            Nearby.Messages.publish(mGoogleApiClient, currentPublishingMessage, options)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(@NonNull Status status) {
                            if (status.isSuccess()) {
                                Log.i(TAG, "Published successfully.");
                            } else {
                                Toast.makeText(getApplicationContext(), "failed to publish", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }

    // subscribe for 30 secs to find other players
    private void startFindingOtherPlayers() {
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
                                Toast.makeText(getApplicationContext(), "subscribed for 15 secs", Toast.LENGTH_SHORT).show();
                                pickGameRunner();
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
                            Toast.makeText(getApplicationContext(), "failed to subscribe", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // pick a device to run the game by choosing the alphabeticallly first user
    private void pickGameRunner() {
        String alphaFirstUser = thisPlayerName;
        for (String user : otherPlayers) {
            if (alphaFirstUser.compareTo(user) > 0) {
                alphaFirstUser = user;
            }
        }

        if (alphaFirstUser.compareTo(thisPlayerName) == 0) {
            //this device is the game runner
            isGameRunner = true;
        }

        //check for potential game runner conflicts
        advertiseGameRunner();

        // then we start a round of the game (2b in the tasks doc)
    }

    private void advertiseGameRunner() {
        if (isGameRunner) {
            // publish "i am the game runner" to other devices
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
                                    Toast.makeText(getApplicationContext(), "published game runner", Toast.LENGTH_SHORT).show();
                                    startGameRound();
                                }
                            });
                        }
                    }).build();

            // publish a message "myname: <name>". other devices will extract the name from the message and add to their list
            currentPublishingMessage = new Message(("gamerunner: " + thisPlayerName).getBytes());
            Nearby.Messages.publish(mGoogleApiClient, currentPublishingMessage, options)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(@NonNull Status status) {
                            if (status.isSuccess()) {
                                Log.i(TAG, "Published successfully.");
                            } else {
                                Toast.makeText(getApplicationContext(), "failed to publish", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }

        // subscribe to recieve game runner message from the game runner
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
                                Toast.makeText(getApplicationContext(), "subscribed for 15 secs", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(getApplicationContext(), "failed to subscribe", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void startGameRound() {
        if (isGameRunner) {
            // randomly select user to be the Message Sender
        }
    }

    /**
     * Creates the dialog box for the user's profile, which allows the user to change his/her
     * public name that gets attached to the message.
     */
    private void createProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        final View dialog_view = inflater.inflate(R.layout.dialog_profile, null);

        builder.setView(dialog_view);

        // Sets the text to the current name
        if (thisPlayerName != "") {

            TextView currentName = (TextView) dialog_view.findViewById(R.id.dialog_profile_current_text);
            Log.i(TAG, "setting current name from " + currentName.getText().toString() + " to " + thisPlayerName);
            currentName.setText(thisPlayerName);
        }

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                EditText nameEntry   = (EditText) dialog_view.findViewById(R.id.dialog_profile_name);

                if( !nameEntry.getText().toString().trim().equals("") ) {
                    thisPlayerName = nameEntry.getText().toString();
                    Log.i(TAG, "player name set to: " + thisPlayerName);
                }
            }
        });

        mProfileDialog = builder.create();
        mProfileDialog.show();
    }

//    private void createMessageDialog() {
//
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//
//        // Get the layout inflater
//        LayoutInflater inflater = this.getLayoutInflater();
//
//        // Inflate and set the layout for the dialog
//        // Pass null as the parent view because its going in the dialog layout
//        final View dialog_view = inflater.inflate(R.layout.dialog_message, null);
//        builder.setView(dialog_view);
//        builder.setPositiveButton(R.string.dialog_message_save, new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int id) {
//                mMessageText   = (EditText) dialog_view.findViewById(R.id.dialog_message);
//                currentPubMessageString = mMessageText.getText().toString();
//                saveCurrentPubMessage();
//                mMessageText.setText("");
//                currentPubMessage = new Message(currentPubMessageString.getBytes());
//
//                Toast.makeText(getApplicationContext(), "Sent message", Toast.LENGTH_LONG).show();
//            }
//        });
//        builder.setNegativeButton(R.string.dialog_message_cancel, null);
//
//
//
//        mMessageDialog = builder.create();
//
//        currPubMessageDisplay = dialog_view.findViewById(R.id.dialog_current_message);
//    }


    // shows the Current Message dialog
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int res_id = item.getItemId();
        if (res_id == R.id.action_message) {

//            if (currentPubMessageString != null && currPubMessageDisplay != null) {
//                currPubMessageDisplay.setText(currentPubMessageString);
//            }
//            else {
//                Log.i(TAG, "no message yet");
//            }
//
//            mMessageDialog.show();
        } else if (res_id == R.id.action_profile) {


            createProfileDialog();
        }
        return true;
    }



//    private void receiveMessage(String newmsg) {
//        mNearbyDevicesArrayAdapter.add( newmsg.toString());
//
//    }

    // TODO this is for testing purposes
    private void receiveFakeMessage() {

        mMessageListener.onFound(new Message("myname: jared".getBytes()));
    }

//    // read from storage to get a current message being published and the list of all received msgs
//    // so they can be displayed
//    private void readFromStorage() {
//        // check if curr msg file already exists & if not create it
//        if (!getFileStreamPath(currMessageStorageFileName).exists()) {
//            try {
//                getFileStreamPath(currMessageStorageFileName).createNewFile();
//            } catch (IOException e) {
//                Log.i(TAG, "IO exception creating curr msg file");
//                e.printStackTrace();
//            }
//        }
//        // read curr msg file
//        try {
//            FileInputStream fis = openFileInput(currMessageStorageFileName);
//            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
//
//            String line;
//            String sep = System.getProperty("line.separator");
//
//            while (null != (line = br.readLine())) {
//                currentPubMessageString = line;
//            }
//
//            br.close();
//
//        } catch (IOException e) {
//            Log.i(TAG, "IOException");
//        }
//
//        //TODO for now fake received messages are added anytime the file is opened here instead of when received
//        // check if received msgs file already exists & if not create it
//        if (!getFileStreamPath(receivedMessagesStorageFileName).exists()) {
//            // read received msgs file
//            try {
//                FileInputStream fis = openFileInput(receivedMessagesStorageFileName);
//                BufferedReader br = new BufferedReader(new InputStreamReader(fis));
//
//                String line;
//                String sep = System.getProperty("line.separator");
//
//                while (null != (line = br.readLine())) {
//                    mNearbyDevicesArrayAdapter.add(line);
//                }
//
//                br.close();
//
//            } catch (IOException e) {
//                Log.i(TAG, "IOException");
//            }
//        }
//    }
//
//    // store all received messages in internal storage
//    private void storeReceivedMessages() {
//
//        // check if received msgs file already exists & if not create it
//        if (!getFileStreamPath(receivedMessagesStorageFileName).exists()) {
//            try {
//                getFileStreamPath(receivedMessagesStorageFileName).createNewFile();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//        }
//        try {
//            FileOutputStream fos = openFileOutput(receivedMessagesStorageFileName, MODE_APPEND);
//
//            PrintWriter pw = new PrintWriter(new BufferedWriter(
//                    new OutputStreamWriter(fos)));
//
//            for (int i=0; i<mNearbyDevicesArrayAdapter.getCount(); i++) {
//                pw.println(mNearbyDevicesArrayAdapter.getItem(i));
//            }
//
//            pw.close();
//        } catch (FileNotFoundException e) {
//            Log.i(TAG, "received messages file not found");
//        }
//    }
//
//    // when the currently broadcasting message is changed, save it to internal storage
//    private void saveCurrentPubMessage() {
//        // check if curr msg file already exists & if not create it, write new str to file
//        if (!getFileStreamPath(currMessageStorageFileName).exists()) {
//            try {
//                getFileStreamPath(currMessageStorageFileName).createNewFile();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        try {
//            FileOutputStream fos = openFileOutput(currMessageStorageFileName, MODE_PRIVATE);
//
//            PrintWriter pw = new PrintWriter(new BufferedWriter(
//                    new OutputStreamWriter(fos)));
//
//            pw.println(currentPubMessageString);
//
//            pw.close();
//        } catch (IOException e) {
//            Log.i(TAG, "IO exception writing to curr msg file");
//            e.printStackTrace();
//        }
//    }

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
//        mPublishSwitch.setEnabled(false);
//        mSubscribeSwitch.setEnabled(false);
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
//        if (mPublishSwitch.isChecked()) {
//            publish();
//        }
//        if (mSubscribeSwitch.isChecked()) {
//            subscribe();
//        }
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "onStop()");
        super.onStop();
//        storeReceivedMessages();
//        saveCurrentPubMessage();

//        if (mPublishSwitch.isChecked())
//            unpublish();
//
//        if (mSubscribeSwitch.isChecked())
//            unsubscribe();
    }
//
//    /**
//     * Subscribes to messages from nearby devices and updates the UI if the subscription either
//     * fails or TTLs.
//     */
//    private void subscribe() {
//        Log.i(TAG, "Subscribing");
//        mNearbyDevicesArrayAdapter.clear();
//        SubscribeOptions options = new SubscribeOptions.Builder()
//                .setStrategy(PUB_SUB_STRATEGY)
//                .setCallback(new SubscribeCallback() {
//                    @Override
//                    public void onExpired() {
//                        super.onExpired();
//                        Log.i(TAG, "No longer subscribing");
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                mSubscribeSwitch.setChecked(false);
//                            }
//                        });
//                    }
//                }).build();
//
//        Nearby.Messages.subscribe(mGoogleApiClient, mMessageListener, options)
//                .setResultCallback(new ResultCallback<Status>() {
//                    @Override
//                    public void onResult(@NonNull Status status) {
//                        if (status.isSuccess()) {
//                            Log.i(TAG, "Subscribed successfully.");
//                        } else {
//                            mSubscribeSwitch.setChecked(false);
//                        }
//                    }
//                });
//    }
//
//    /**
//     * Publishes a message to nearby devices and updates the UI if the publication either fails or
//     * TTLs.
//     */
//    private void publish() {
//        Log.i(TAG, "Publishing");
//        PublishOptions options = new PublishOptions.Builder()
//                .setStrategy(PUB_SUB_STRATEGY)
//                .setCallback(new PublishCallback() {
//                    @Override
//                    public void onExpired() {
//                        super.onExpired();
//                        Log.i(TAG, "No longer publishing");
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                mPublishSwitch.setChecked(false);
//                            }
//                        });
//                    }
//                }).build();
//
//        if (currentPubMessage == null) {
//            Log.i(TAG, "message was null");
//            //currentPubMessageString = "No message is being published yet";
//            currentPubMessage = new Message(currentPubMessageString.getBytes());
//        }
//        else {
//        }
//        Nearby.Messages.publish(mGoogleApiClient, currentPubMessage, options)
//                .setResultCallback(new ResultCallback<Status>() {
//                    @Override
//                    public void onResult(@NonNull Status status) {
//                        if (status.isSuccess()) {
//                            Log.i(TAG, "Published successfully.");
//                        } else {
//                            Log.i(TAG, status.getStatus().toString());
//                            Log.i(TAG, status.getStatusMessage());
//                            Log.i(TAG, status.getStatusCode() + "");
//                            mPublishSwitch.setChecked(false);
//                        }
//                    }
//                });
//    }

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
        Nearby.Messages.unpublish(mGoogleApiClient, currentPublishingMessage);
    }

}