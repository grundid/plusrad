/**
 * Cycle Philly, Copyright 2014 Code for Philly
 *
 * @author Lloyd Emelle <lloyd@codeforamerica.org>
 * @author Christopher Le Dantec <ledantec@gatech.edu>
 * @author Anhong Guo <guoanhong15@gmail.com>
 * <p>
 * Updated/Modified for Philly's app deployment. Based on the
 * CycleTracks codebase for SFCTA and Cycle Atlanta.
 * <p>
 * CycleTracks, Copyright 2009,2010 San Francisco County Transportation Authority
 * San Francisco, CA, USA
 * @author Billy Charlton <billy.charlton@sfcta.org>
 * <p>
 * This file is part of CycleTracks.
 * <p>
 * CycleTracks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * CycleTracks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with CycleTracks.  If not, see <http://www.gnu.org/licenses/>.
 */
//
package de.grundid.plusrad.map;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import de.grundid.plusrad.R;
import de.grundid.plusrad.recording.CyclePoint;
import de.grundid.plusrad.recording.DbAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class ShowMap extends AppCompatActivity {

	private GoogleMap map;
	private long tripId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mapview);
		tripId = getIntent().getLongExtra("TRIP_ID", -1);
		if (tripId == -1) {
			finish();
		}
		((SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(
				new OnMapReadyCallback() {

					@Override
					public void onMapReady(GoogleMap googleMap) {
						continueWithMap(googleMap);
					}
				});
	}

	private void continueWithMap(GoogleMap googleMap) {
		this.map = googleMap;

/*		TripData trip = TripData.fetchTrip(this, tripid);
		// map bounds
		final LatLngBounds bounds = new LatLngBounds.Builder()
				.include(new LatLng(trip.lathigh, trip.lgtlow))
				.include(new LatLng(trip.latlow, trip.lgthigh))
				.build();
		ViewTreeObserver vto = layout.getViewTreeObserver();
		vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

			@Override
			public void onGlobalLayout() {
				layout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
				// Center & zoom the map after map layout completes
				mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 5));
			}
		});*/
		// customize info window
		//map.setInfoWindowAdapter(new BikeRackInfoWindow(getLayoutInflater()));
		AddPointsToMapLayerTask maptask = new AddPointsToMapLayerTask(this);
		maptask.execute(tripId);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_show_map, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_share) {
			TrackToGeoJsonExport task = new TrackToGeoJsonExport(this);
			task.execute(tripId);
			return true;
		} else
			return super.onOptionsItemSelected(item);
	}

	private class AddPointsToMapLayerTask extends AsyncTask<Long, Integer, PolylineOptions> {

		private Context context;

		private AddPointsToMapLayerTask(Context context) {
			this.context = context;
		}

		@Override
		protected PolylineOptions doInBackground(Long... trips) {
			Log.i("PRAD", "Loading points");
			DbAdapter dbAdapter = new DbAdapter(context);
			List<CyclePoint> points = dbAdapter.fetchAllCoordsForTrip(tripId);
			PolylineOptions polygonOptions = new PolylineOptions().color(0xFF29B6F6).width(20);
			for (CyclePoint point : points) {
				polygonOptions.add(point.getCoords());
			}
			Log.i("PRAD", "Loading done");
			return polygonOptions;
		}

		@Override
		protected void onPostExecute(PolylineOptions opts) {
			Log.i("PRAD", "Adding polyline to map");
			map.addPolyline(opts);
			List<LatLng> points = opts.getPoints();
			LatLng startPoint = points.get(0);
			LatLng endPoint = points.get(points.size() - 1);
			map.moveCamera(CameraUpdateFactory.newLatLngZoom(startPoint, 14));
			Log.i("PRAD", "Adding polyline to map, done");
			map.addMarker(new MarkerOptions()
					.position(startPoint)
					.title("Startpunkt")
					.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
			map.addMarker(new MarkerOptions()
					.position(endPoint)
					.title("Ende")
					.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
		}
	}

	public void performExport(String geoJsonData) {
		try {
			File exportFile = createTrackFile(geoJsonData);
			Intent shareIntent = new Intent();
			shareIntent.setAction(Intent.ACTION_SEND);
			Uri shareUri = FileProvider
					.getUriForFile(this, "de.grundid.plusrad.fileprovider", exportFile);
			shareIntent.setData(shareUri);
			shareIntent.putExtra(Intent.EXTRA_STREAM, shareUri);
			shareIntent.setType("text/comma_separated_values/csv");
			shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			startActivity(Intent.createChooser(shareIntent, "Track exportieren"));
		}
		catch (IOException e) {
			e.printStackTrace();
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Fehler")
					.setMessage("Fehler beim Exportieren des Tracks. [" + e.getMessage() + "]");
			builder.setPositiveButton("OK", null).show();
		}
	}

	private File createTrackFile(String geoJsonData) throws IOException {
		File imagePath = new File(getFilesDir(), "tracks");
		if (!imagePath.exists()) {
			imagePath.mkdirs();
		}
		File newFile = new File(imagePath, "track-" + tripId + ".geojson");
		FileOutputStream out = new FileOutputStream(newFile);
		out.write(geoJsonData.getBytes("utf8"));
		out.flush();
		out.close();
		return newFile;
	}
}
