package de.grundid.plusrad.recording;

import android.content.*;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import de.grundid.plusrad.R;

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

@SuppressWarnings("MissingPermission")
public class RecordingActivity extends AppCompatActivity {

	private IntentFilter mIntentFilter;
	private ServiceConnection serviceConnection;
	Button pauseButton;
	Button finishButton;
	Timer timer;
	TextView distance;
	TextView duration;
	TextView currentSpeed;
	TextView maxSpeed;
	TextView avgSpeedRideTime;
	TextView avgSpeedTotalTime;
	TextView standingTime;
	TextView ridingTime;
	private View loader;
	final SimpleDateFormat sdf = new SimpleDateFormat("H:mm:ss");
	final Handler mHandler = new Handler();
	final Runnable mUpdateTimer = new Runnable() {

		public void run() {
			updateTimer();
		}
	};
	private RecordingService.IRecordService recordService;
	private GoogleMap googleMap;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.recording);
		distance = (TextView)findViewById(R.id.distance);
		duration = (TextView)findViewById(R.id.duration);
		currentSpeed = (TextView)findViewById(R.id.currentSpeed);
		maxSpeed = (TextView)findViewById(R.id.maxSpeed);
		avgSpeedRideTime = (TextView)findViewById(R.id.avgRideTime);
		avgSpeedTotalTime = (TextView)findViewById(R.id.avgTotalTime);
		standingTime = (TextView)findViewById(R.id.standingTime);
		ridingTime = (TextView)findViewById(R.id.ridingTime);
		pauseButton = (Button)findViewById(R.id.ButtonPause);
		finishButton = (Button)findViewById(R.id.ButtonFinished);
		loader = findViewById(R.id.loader);
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		mIntentFilter = new IntentFilter(ACTIVITY_SERVICE);
		mIntentFilter.addCategory(NOTIFICATION_SERVICE);
		// Query the RecordingService to figure out what to do.
		Intent rService = new Intent(this, RecordingService.class);
		startService(rService);
		// Pause button
		((SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(
				new OnMapReadyCallback() {

					@Override
					public void onMapReady(GoogleMap googleMap) {
						continueWithMap(googleMap);
					}
				});
		pauseButton.setEnabled(false);
		pauseButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				if (recordService.getState() == RecordingService.STATE_RECORDING) {
					pauseButton.setText("Pause");
					RecordingActivity.this.setTitle("PlusRad - Recording...");
					Toast.makeText(getBaseContext(), "GPS restarted. It may take a moment to resync.",
							Toast.LENGTH_LONG).show();
				} else {
					pauseButton.setText("Resume");
					RecordingActivity.this.setTitle("PlusRad - Paused...");
					Toast.makeText(getBaseContext(), "Recording paused; GPS now offline", Toast.LENGTH_LONG).show();
				}
			}
		});
		// Finish button
		finishButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				// If we have points, go to the save-trip activity
				CurrentTrip trip = recordService.getCurrentTrip();
				recordService.finishRecording();
				finish();
				if (trip.hasTrackData()) {
				}
				// Otherwise, cancel and go back to main screen
				else {
					Toast.makeText(getBaseContext(), "No GPS data acquired; nothing to submit.", Toast.LENGTH_SHORT)
							.show();
					recordService.cancelRecording();
				}
				// Either way, activate next task, and then kill this task
				//startActivity(new Intent());
			}
		});
	}

	private void continueWithMap(GoogleMap googleMap) {
		this.googleMap = googleMap;
		googleMap.setMyLocationEnabled(true);
	}

	@Override
	protected void onStart() {
		super.onStart();
		Intent rService = new Intent(this, RecordingService.class);
		serviceConnection = new ServiceConnection() {

			public void onServiceDisconnected(ComponentName name) {
				// TODO disable buttons
				Log.i("PRAD", "onServiceDisconnected: " + name);
			}

			public void onServiceConnected(ComponentName name, IBinder service) {
				recordService = (RecordingService.IRecordService)service;
				switch (recordService.getState()) {
					case RecordingService.STATE_IDLE:
						recordService.startRecording();
						RecordingActivity.this.pauseButton.setEnabled(true);
						RecordingActivity.this.setTitle("PlusRad - Aufzeichnung...");
						break;
					case RecordingService.STATE_RECORDING:
						RecordingActivity.this.pauseButton.setEnabled(true);
						RecordingActivity.this.setTitle("PlusRad - Aufzeichnung...");
						break;
					case RecordingService.STATE_PAUSED:
						recordService.getCurrentTrip();
						RecordingActivity.this.pauseButton.setEnabled(true);
						RecordingActivity.this.pauseButton.setText("Resume");
						RecordingActivity.this.setTitle("PlusRad - Pausiert...");
						break;
				}
			}
		};
		bindService(rService, serviceConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onStop() {
		super.onStop();
		unbindService(serviceConnection);
	}

	// onResume is called whenever this activity comes to foreground.
	// Use a timer to update the trip duration.
	@Override
	public void onResume() {
		super.onResume();
		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				mHandler.post(mUpdateTimer);
			}
		}, 0, 1000);  // every second
	}

	void updateTimer() {
		if (recordService != null && recordService.getState() == RecordingService.STATE_RECORDING) {
			CurrentTrip currentTrip = recordService.getCurrentTrip();
			duration.setText(sdf.format(currentTrip.getTripDuration()));
			currentSpeed.setText(String.format("%1.1f km/h", currentTrip.getCurrentSpeed() * 36 / 10));
			maxSpeed.setText(String.format("%1.1f km/h", currentTrip.getMaxSpeed() * 36 / 10));
			distance.setText(String.format("%1.1f km", currentTrip.getDistanceTraveled() / 1000));
			double avgRidingTime = currentTrip.getAvgRidingTime();
			if (Double.isNaN(avgRidingTime)) {
				avgSpeedRideTime.setText("-");
			} else {
				avgSpeedRideTime.setText(String.format("%1.1f km/h", avgRidingTime * 36 / 10));
			}
			double avgTotalTime = currentTrip.getAvgTotalTime();
			if (Double.isNaN(avgTotalTime)) {
				avgSpeedTotalTime.setText("-");
			} else {
				avgSpeedTotalTime.setText(String.format("%1.1f km/h", avgTotalTime * 36 / 10));
			}
			ridingTime.setText(sdf.format(currentTrip.getRindingTime()));
			standingTime.setText(sdf.format(currentTrip.getStandingTime()));
			Location location = currentTrip.getLocation();
			if (googleMap != null && location != null) {
				LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
				googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (timer != null)
			timer.cancel();
	}
}
