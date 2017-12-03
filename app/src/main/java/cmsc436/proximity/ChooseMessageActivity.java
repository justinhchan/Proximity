package cmsc436.proximity;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Kevin on 12/2/17.
 */

public class ChooseMessageActivity extends Activity {
    ArrayList<String> players;
    private ArrayAdapter<String> mPlayersListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.choose_message);
        Bundle extras = getIntent().getExtras();
        players = extras.getStringArrayList("players");
        mPlayersListAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                players);

        ListView playersListView = (ListView) findViewById(
                R.id.playersListView);

        TextView emptyText = (TextView) findViewById(R.id.emptyT);
        playersListView.setEmptyView(emptyText);
        playersListView.setAdapter(mPlayersListAdapter);
    }
}
