package de.grundid.plusrad.list;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import de.grundid.plusrad.ItemClickListener;
import de.grundid.plusrad.R;
import de.grundid.plusrad.map.ShowMap;
import de.grundid.plusrad.recording.DbAdapter;
import de.grundid.plusrad.db.TripData;

import java.util.List;

public class TripListActivity extends AppCompatActivity implements ItemClickListener<TripData> {

	private RecyclerView recyclerView;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.trip_list);
		recyclerView = (RecyclerView)findViewById(R.id.recyclerView);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		setTitle("Bisherige Touren");
		DbAdapter dbAdapter = new DbAdapter(this);
		List<TripData> allTrips = dbAdapter.getAllTrips();
		recyclerView.setAdapter(new TripListAdapter(allTrips, this));
	}

	@Override
	public void onItemClicked(TripData item) {
		Intent intent = new Intent(this, ShowMap.class);
		intent.putExtra("TRIP_ID", item.getTripId());
		startActivity(intent);
	}
}
