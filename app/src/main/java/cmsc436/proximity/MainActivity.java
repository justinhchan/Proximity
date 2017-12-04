package cmsc436.proximity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
    private static final int GUESSING_REQUEST = 1;
    private static final int TTL_IN_SECONDS = 60;

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
    private AlertDialog mMessageDialog;

    /**
     * A {@link MessageListener} for processing messages from nearby devices.
     */
    private MessageListener mMessageListener;

    /**
     * Adapter for working with messages from nearby publishers.
     */
    private ArrayAdapter<String> mNearbyDevicesArrayAdapter;

    /* Variables for current objects */
    private String mCurrentUser = "";
    private DeviceMessage mCurrentMessage;
    private Message mCurrentPublishingMessage;
    private int mHighScore;
    private int mCurrentScore;

    private ArrayList<String> otherPlayers;
    private Map<String, String> messagesAndPlayers;

    private AlertDialog mProfileDialog;
    private TextView highScoreTextView;
    private Boolean isGameRunner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar)findViewById(R.id.action_bar);
        setSupportActionBar(mToolbar);

        otherPlayers = new ArrayList<String>();
        messagesAndPlayers = new HashMap<String, String>();
        mHighScore = 0;
        mCurrentScore = 0;
        isGameRunner = false;

//        createMessageDialog();

        mMessageListener = new MessageListener() {
            @Override
            public void onFound(final Message message) {
                String messageContent = new String(message.getContent());
                // Called when a new message is found.
                Log.i(TAG, "received message " + messageContent);
                String[] messageParts = messageContent.split(Integer.toString(TAG.hashCode()));

                String otherPlayerName = messageParts[0];
                String otherPlayerMessage = messageParts[1];

                messagesAndPlayers.put(otherPlayerMessage, otherPlayerName);
                otherPlayers.add(otherPlayerName);
                Log.i(TAG, "message: " + otherPlayerMessage + " name: " + otherPlayerName);
                mNearbyDevicesArrayAdapter.add(otherPlayerMessage);

            }

            @Override
            public void onLost(final Message message) {
                // Called when a message is no longer detectable nearby.
//                mNearbyDevicesArrayAdapter.remove("user");
                Toast.makeText(getApplicationContext(), "Message expired", Toast.LENGTH_SHORT).show();
            }
        };

        Button startGameBtn = findViewById(R.id.startgamebtn);
        startGameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCurrentUser.equals("") || mCurrentMessage == null) {
                    Toast.makeText(getApplicationContext(), "BOTH your name and message need to be set before starting.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Game has started", Toast.LENGTH_SHORT).show();
                    startGame();
                }
            }
        });

        Button clearListBtn = findViewById(R.id.clearbtn);
        clearListBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mNearbyDevicesArrayAdapter.clear();
                Log.i(TAG, "Cleared list of received messages.");
            }
        });

        final ArrayList<String> nearbyDevicesArrayList = new ArrayList<>();
        mNearbyDevicesArrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                nearbyDevicesArrayList);
        final ListView nearbyDevicesListView = (ListView) findViewById(
                R.id.nearby_devices_list_view);

        // Using ListActivity's setEmptyView method automatically
        // checks if the adapter is empty or not. If the adapter is
        // empty, then is displays "No messages received." Otherwise,
        // it populates the ListView with messages from the array.
        TextView emptyText = (TextView) findViewById(R.id.emptyT);
        nearbyDevicesListView.setEmptyView(emptyText);
        nearbyDevicesListView.setAdapter(mNearbyDevicesArrayAdapter);

        highScoreTextView = (TextView) findViewById(R.id.highscore);
        highScoreTextView.setText("Current high score: " + Integer.toString(mHighScore));

        // When a message in the ListView is selected, the user is sent to
        // a page that displays the message and a list of possible users who
        // could've sent that message.
        // TODO: identify the original sender of the message and send that information
        // into the activity
        nearbyDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Intent intent = new Intent(MainActivity.this, ChooseMessageActivity.class);
                Bundle mBundle = new Bundle();
                // Retrieve otherPlayers list to send into the new activity
                mBundle.putStringArrayList("otherPlayers", otherPlayers);
                // Message that was selected
                String msg = (String) nearbyDevicesListView.getItemAtPosition(position);
                mBundle.putString("message", msg);
                // Log.i(TAG, "Current user is " + mCurrentUser);
                // Player who sent the message
                String sender = messagesAndPlayers.get(msg);
                mBundle.putString("originalSender", sender);
                Log.i(TAG, "start choosemessage activity message: " + msg + " sender: " + sender);
                intent.putExtras(mBundle);
                // Should be startActivity for result which indicates
                // a score based on correct guess
                startActivityForResult(intent, GUESSING_REQUEST);
            }
        });
        buildGoogleApiClient();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Activity returned successfully
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GUESSING_REQUEST) {
                int point = data.getIntExtra("point", 0);
                // If user has guessed correctly, add a point to their current
                // streak.
                if (point == 1) {
                    Log.i(TAG, "User guessed correctly and received " + point + " point");
                    mCurrentScore += point;
                    Toast.makeText(getApplicationContext(), "Correct!", Toast.LENGTH_SHORT).show();
                // Else, the user has guessed incorrectly and their streak is reset
                // back to 0.
                } else {
                    Log.i(TAG, "User guessed incorrectly and score is reset");

                    Toast.makeText(getApplicationContext(), "Incorrect!", Toast.LENGTH_SHORT).show();
                    mCurrentScore = 0;
                }

                // If current streak is greater than the high score, then update
                // with the new high score.
                if (mCurrentScore > mHighScore) {
                    mHighScore = mCurrentScore;
                }

                if (highScoreTextView == null) {
                    Log.i(TAG, "updating high score, textview is null");
                }
                highScoreTextView.setText("Current high score: " + Integer.toString(mHighScore));

                String messageGuessed = data.getStringExtra("message");
                mNearbyDevicesArrayAdapter.remove(messageGuessed);
            }
        // Toast/ Message saying activity was cancelled.
        // User did not guess a user
        } else {
            Log.i(TAG, "Activity was cancelled");
        }
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
        publishNameAndMessage();
        startFindingOtherPlayers();
//        for (int i = 0; i < 5; i++) {
//            String tempName = mCurrentUser + i;
//            DeviceMessage testMessageObj = new DeviceMessage(tempName, "test " + i + " from " + tempName);
//            sendMessage(testMessageObj);
////            sendMessage(mCurrentMessage);
//        }
    }

    private void sendMessage(DeviceMessage msgObj) {
        otherPlayers.add(msgObj.getSender());
        mMessageListener.onFound(msgObj.getMessageBody());
    }

    // publish this player's name to nearby devices for 30 secs
    private void publishNameAndMessage() {
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


            mCurrentPublishingMessage = new Message((mCurrentUser + TAG.hashCode() + mCurrentMessage.getMessageString()).getBytes());
            Log.i(TAG, "sending msg " + new String(mCurrentPublishingMessage.getContent()));
            Nearby.Messages.publish(mGoogleApiClient, mCurrentPublishingMessage, options)
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
//                                pickGameRunner();
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

                            Log.i(TAG, status.getStatus().toString());
                            Log.i(TAG, status.getStatusMessage());
                            Log.i(TAG, status.getStatusCode() + "");
                            Toast.makeText(getApplicationContext(), "failed to subscribe", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // pick a device to run the game by choosing the alphabeticallly first user
    private void pickGameRunner() {
        String alphaFirstUser = mCurrentUser;
        for (String user : otherPlayers) {
            if (alphaFirstUser.compareTo(user) > 0) {
                alphaFirstUser = user;
            }
        }

        if (alphaFirstUser.compareTo(mCurrentUser) == 0) {
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
            mCurrentPublishingMessage = new Message(("gamerunner: " + mCurrentUser).getBytes());
            Nearby.Messages.publish(mGoogleApiClient, mCurrentPublishingMessage, options)
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
        
        // Sets the text to the current name
        if (!mCurrentUser.equals("")) {

            TextView currentName = (TextView) dialog_view.findViewById(R.id.dialog_profile_current_text);
            Log.i(TAG, "setting current name from " + currentName.getText().toString() + " to " + mCurrentUser);
            currentName.setText(mCurrentUser);
        }

        // Sets the text to the current high score
        TextView profileScoreView = (TextView) dialog_view.findViewById(R.id.dialog_profile_score);
        int profileScore = Integer.parseInt(profileScoreView.getText().toString());
        if (mHighScore != profileScore) {
            profileScoreView.setText(Integer.toString(mHighScore));
            Log.i(TAG, "set new high score to " + mHighScore);
        }

        builder.setView(dialog_view);

        builder.setPositiveButton(R.string.dialog_message_save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                EditText nameEntry = (EditText) dialog_view.findViewById(R.id.dialog_profile_name);

                if( nameEntry.getText().toString().trim().equals("") ) {
                    Toast.makeText(getApplicationContext(), "Invalid name.", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    mCurrentUser = nameEntry.getText().toString().trim();
                    Log.i(TAG, "player name set to: " + mCurrentUser);
                    Toast.makeText(getApplicationContext(), "Name set to " + mCurrentUser, Toast.LENGTH_SHORT).show();
                }
            }
        });

        mProfileDialog = builder.create();
        mProfileDialog.show();
    }

    private void createMessageDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        final View dialog_view = inflater.inflate(R.layout.dialog_message, null);

        if (mCurrentMessage != null) {

            String currentMessageString = mCurrentMessage.getMessageString();

            TextView currentMessageView = (TextView) dialog_view.findViewById(R.id.dialog_message_current_text);
            Log.i(TAG, "setting current message from " + currentMessageView.getText().toString() + " to " + currentMessageString);
            currentMessageView.setText(currentMessageString);
        }
        builder.setView(dialog_view);

        builder.setPositiveButton(R.string.dialog_message_save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {

                EditText messageEntry = (EditText) dialog_view.findViewById(R.id.dialog_message_message);
                if (mCurrentUser.equals("")) {
                    Toast.makeText(getApplicationContext(), "Please set your name first.", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else if (messageEntry.getText().toString().trim().equals("")) {
                    Toast.makeText(getApplicationContext(), "Invalid message.", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    mCurrentMessage = new DeviceMessage(mCurrentUser, messageEntry.getText().toString());
                    mCurrentPublishingMessage = mCurrentMessage.getMessageBody();
                    Log.i(TAG, "setting new message: " + mCurrentMessage.getMessageString());
                    Toast.makeText(getApplicationContext(), "Message set to " + mCurrentMessage.getMessageString(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        mMessageDialog = builder.create();
        mMessageDialog.show();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int res_id = item.getItemId();
        if (res_id == R.id.action_message) {

          createMessageDialog();

        } else if (res_id == R.id.action_profile) {

            createProfileDialog();
        }
        return true;
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
                    }
                }).build();

        Nearby.Messages.subscribe(mGoogleApiClient, mMessageListener, options)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Subscribed successfully.");
                        } else {

                            Log.i(TAG, status.getStatus().toString());
                            Log.i(TAG, status.getStatusMessage());
                            Log.i(TAG, status.getStatusCode() + "");
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
                            }
                        });
                    }
                }).build();


        Nearby.Messages.publish(mGoogleApiClient, mCurrentPublishingMessage, options)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Published successfully.");
                        } else {
                            Log.i(TAG, status.getStatus().toString());
                            Log.i(TAG, status.getStatusMessage());
                            Log.i(TAG, status.getStatusCode() + "");
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
        Nearby.Messages.unpublish(mGoogleApiClient, mCurrentPublishingMessage);
    }

}