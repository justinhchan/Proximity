package cmsc436.proximity;

import android.app.Activity;
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
    private String gameRunner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.choose_message);
        Bundle extras = getIntent().getExtras();
        otherPlayers = extras.getStringArrayList("otherPlayers");
        message = extras.getString("message");
        gameRunner = extras.getString("gameRunner");
        mPlayersListAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                otherPlayers);

        ListView playersListView = (ListView) findViewById(
                R.id.playersListView);

        TextView emptyText = (TextView) findViewById(R.id.emptyPlayers);
        playersListView.setEmptyView(emptyText);
        playersListView.setAdapter(mPlayersListAdapter);

        final TextView headerView = (TextView) findViewById(R.id.top_header);
        headerView.setText("Guess which player sent the message: ");
        //TODO: retrieve message the user has clicked

        // User gets one guess to choose to correct player
        playersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                /*
                 if (correct player was guessed) {
                    return activity with score of 1
                 } else {
                    return activity with a score of 0
                 }
                 */
            }
        });
    }
}
