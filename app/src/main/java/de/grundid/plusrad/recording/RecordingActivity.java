package de.grundid.plusrad.recording;

import android.content.*;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
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

	final SimpleDateFormat sdf = new SimpleDateFormat("H:mm:ss");
	final Handler mHandler = new Handler();
	Button pauseButton;
	Button finishButton;
	Timer timer;
	private IntentFilter mIntentFilter;
	private ServiceConnection serviceConnection;
	private TextView message;
	private TextView distance;
	private TextView duration;
	private TextView currentSpeed;
	private TextView maxSpeed;
	private TextView avgSpeedRideTime;
	private TextView avgSpeedTotalTime;
	private TextView standingTime;
	private TextView ridingTime;
	private RecordingService.IRecordService recordService;
	private GoogleMap googleMap;
	final Runnable mUpdateTimer = new Runnable() {

		public void run() {
			updateTimer();
		}
	};
	private LocalBroadcastManager broadcastManager;
	private BroadcastReceiver broadcastReceiver;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.recording);
		message = (TextView)findViewById(R.id.message);
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
					recordService.pauseRecording();
					pauseButton.setText("Fortsetzen");
					RecordingActivity.this.setTitle("PlusRad - Pausiert...");
					Toast.makeText(getBaseContext(), "Aufzeichnung pausiert", Toast.LENGTH_LONG).show();
				} else if (recordService.getState() == RecordingService.STATE_PAUSED) {
					recordService.resumeRecording();
					pauseButton.setText("Pause");
					RecordingActivity.this.setTitle("PlusRad - Aufzeichnung...");
					Toast.makeText(getBaseContext(),
							"GPS aktiviert. Es kann einen Augenblick dauern bis die Position verfügbar ist",
							Toast.LENGTH_LONG).show();
				}
			}
		});
		// Finish button
		finishButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				// If we have points, go to the save-trip activity
				CurrentTrip trip = recordService.getCurrentTrip();
				recordService.finishRecording();
				if (trip.hasTrackData()) {
				}
				// Otherwise, cancel and go back to main screen
				else {
					Toast.makeText(getBaseContext(), "No GPS data acquired; nothing to submit.", Toast.LENGTH_SHORT)
							.show();
					recordService.cancelRecording();
				}
				finish();
				// Either way, activate next task, and then kill this task
				//startActivity(new Intent());
			}
		});
		broadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
		broadcastReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				String activityName = intent.getStringExtra("activityName");
				int confidence = intent.getIntExtra("confidence", -1);
				Toast.makeText(context, "Activity: " + activityName + " (" + confidence + ")", Toast.LENGTH_SHORT)
						.show();
				ActionBar supportActionBar = getSupportActionBar();
				if (supportActionBar != null) {
					supportActionBar.setSubtitle(activityName + " (" + confidence + ")");
				}
			}
		};
	}

	private void continueWithMap(GoogleMap googleMap) {
		this.googleMap = googleMap;
		googleMap.setMyLocationEnabled(true);
		LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		Location lastKnownLocation = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
		if (lastKnownLocation != null) {
			LatLng latLng = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
			googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
		}
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
						RecordingActivity.this.pauseButton.setText("Fortsetzen");
						RecordingActivity.this.setTitle("PlusRad - Pausiert...");
						break;
				}
			}
		};
		bindService(rService, serviceConnection, Context.BIND_AUTO_CREATE);
		IntentFilter intentFilter = new IntentFilter("activityUpdate");
		broadcastManager.registerReceiver(broadcastReceiver, intentFilter);
	}

	@Override
	protected void onStop() {
		super.onStop();
		unbindService(serviceConnection);
		broadcastManager.unregisterReceiver(broadcastReceiver);
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
			if (currentTrip.getDistanceTraveled() < 1000) {
				distance.setText(String.format("%1.0f m", currentTrip.getDistanceTraveled()));
			} else {
				distance.setText(String.format("%1.1f km", currentTrip.getDistanceTraveled() / 1000));
			}
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
			message.setVisibility(CurrentTrip.isLocationAccurate(location) ? View.GONE : View.VISIBLE);
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
