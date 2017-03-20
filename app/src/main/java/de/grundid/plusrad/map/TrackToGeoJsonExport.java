package de.grundid.plusrad.map;

import android.os.AsyncTask;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.LineString;
import org.geojson.LngLatAlt;
import de.grundid.plusrad.recording.CyclePoint;
import de.grundid.plusrad.recording.DbAdapter;

import java.util.List;

public class TrackToGeoJsonExport extends AsyncTask<Long, Void, String> {

	private ShowMap context;

	public TrackToGeoJsonExport(ShowMap context) {
		this.context = context;
	}

	@Override
	protected String doInBackground(Long... params) {
		try {
			DbAdapter dbAdapter = new DbAdapter(context);
			List<CyclePoint> points = dbAdapter.fetchAllCoordsForTrip(params[0]);
			FeatureCollection fc = new FeatureCollection();
			// TODO add properties about the trip
			Feature track = new Feature();
			LineString lineString = new LineString();
			for (CyclePoint point : points) {
				LngLatAlt geoPoint = new LngLatAlt(point.getCoords().longitude, point.getCoords().latitude,
						point.getAltitude());
				lineString.add(geoPoint);
			}
			track.setGeometry(lineString);
			fc.add(track);
			ObjectMapper objectMapper = new ObjectMapper();
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(fc);
		}
		catch (JsonProcessingException e) {
			cancel(false);
			return null;
		}
	}

	@Override
	protected void onPostExecute(String s) {
		context.performExport(s);
	}
}
