package de.grundid.plusrad.list;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import de.grundid.plusrad.ItemClickListener;
import de.grundid.plusrad.R;
import de.grundid.plusrad.db.TripData;

import java.util.List;

public class TripListAdapter extends RecyclerView.Adapter<TripViewHolder> {

	private List<TripData> trips;
	private ItemClickListener<TripData> clickListener;

	public TripListAdapter(List<TripData> trips, ItemClickListener<TripData> clickListener) {
		this.trips = trips;
		this.clickListener = clickListener;
	}

	@Override
	public TripViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
		return new TripViewHolder(layoutInflater.inflate(R.layout.trip_item, parent, false));
	}

	@Override
	public void onBindViewHolder(TripViewHolder holder, int position) {
		final TripData tripData = trips.get(position);
		holder.update(tripData);
		holder.itemView.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				clickListener.onItemClicked(tripData);
			}
		});
	}

	@Override
	public int getItemCount() {
		return trips.size();
	}
}
