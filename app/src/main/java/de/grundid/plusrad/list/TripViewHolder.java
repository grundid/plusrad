package de.grundid.plusrad.list;

import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.widget.TextView;
import de.grundid.plusrad.R;
import de.grundid.plusrad.db.TripData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class TripViewHolder extends ViewHolder {

	private TextView title;
	private TextView distance;
	private TextView ridingTime;
	private TextView avgRideTime;
	final SimpleDateFormat ridingTimeFormatter = new SimpleDateFormat("H:mm:ss");
	final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
	final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

	public TripViewHolder(View itemView) {
		super(itemView);
		title = (TextView)itemView.findViewById(R.id.title);
		distance = (TextView)itemView.findViewById(R.id.distance);
		ridingTime = (TextView)itemView.findViewById(R.id.ridingTime);
		avgRideTime = (TextView)itemView.findViewById(R.id.avgRideTime);
		ridingTimeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	public void update(TripData tripData) {
		long tripTime = tripData.getTripTime();
		title.setText(dateFormat.format(new Date(tripData.getStartTime())) + " von " + timeFormat
				.format(new Date(tripData.getStartTime())) + " bis " +
				timeFormat.format(new Date(tripData.getEndTime())));
		distance.setText(String.format("%1.1f km", tripData.getDistance() / 1000));

		double avgRidingTime = tripData.getDistance() / (tripTime / 1000);
		if (avgRidingTime > 0 && !Double.isInfinite(avgRidingTime)) {
			avgRideTime.setText(String.format("%1.1f km/h", avgRidingTime * 36 / 10));
		} else {
			avgRideTime.setText("-");
		}
		ridingTime.setText(ridingTimeFormatter.format(tripTime));
	}
}
