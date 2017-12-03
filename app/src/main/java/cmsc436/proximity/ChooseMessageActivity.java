package cmsc436.proximity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Kevin on 12/2/17.
 */

public class ChooseMessageActivity extends Activity {
    ArrayList<String> otherPlayers;
    private ArrayAdapter<String> mPlayersListAdapter;
    private String message;
    private String sender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.choose_message);
        Bundle extras = getIntent().getExtras();
        otherPlayers = extras.getStringArrayList("otherPlayers");
        message = extras.getString("message");
        sender = extras.getString("originalSender");
        mPlayersListAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                otherPlayers);

        final ListView playersListView = (ListView) findViewById(
                R.id.playersListView);

        TextView emptyText = (TextView) findViewById(R.id.emptyPlayers);
        playersListView.setEmptyView(emptyText);
        playersListView.setAdapter(mPlayersListAdapter);

        final TextView headerView = (TextView) findViewById(R.id.top_header);
        headerView.setText("Guess which player sent the message: " + message);
        //TODO: retrieve message the user has clicked

        // User gets one guess to choose to correct player
        playersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                String player = (String) playersListView.getItemAtPosition(position);

                Intent returnIntent = new Intent();

                // If the user correctly guesses who sent the message
                if (player.equals(sender)) {
                    returnIntent.putExtra("point", 1);
                // Else, the user's guess was wrong
                } else {
                    returnIntent.putExtra("point", 0);
                }

                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }
        });
    }
}
