package de.grundid.plusrad;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import de.grundid.plusrad.db.TripData;
import de.grundid.plusrad.list.TripListActivity;
import de.grundid.plusrad.recording.*;
import de.grundid.plusrad.stats.Stats;

public class MainActivity extends AppCompatActivity {

	public static final int FINE_LOCATION_REQUEST_CODE = 1000;
	private final static int MENU_USER_INFO = 0;
	private final static int MENU_CONTACT_US = 1;
	private final static int MENU_MAP = 2;
	private final static int MENU_LEGAL_INFO = 3;
	public final static int PREF_ANONID = 13;
	private final static int CONTEXT_RETRY = 0;
	private final static int CONTEXT_DELETE = 1;
	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case CONNECTION_FAILURE_RESOLUTION_REQUEST:
				switch (resultCode) {
					case Activity.RESULT_OK:
						//TODO: ...try the request again?
						break;
				}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		connectToService();
		final Button startButton = (Button)findViewById(R.id.ButtonStart);
		startButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION)
						== PackageManager.PERMISSION_GRANTED) {
					proccessWithLocationPermission();
				} else {
					ActivityCompat.requestPermissions(MainActivity.this,
							new String[] { android.Manifest.permission.ACCESS_FINE_LOCATION },
							FINE_LOCATION_REQUEST_CODE);
				}
			}
		});
		findViewById(R.id.showTripList).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(MainActivity.this, TripListActivity.class));
			}
		});
		Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
		toolbar.setTitle(R.string.app_name);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayShowTitleEnabled(true);
		Stats stats = createStats();
	}

	private Stats createStats() {
		DbAdapter dbAdapter = new DbAdapter(this);
		Stats stats = new Stats();
		for (TripData tripData : dbAdapter.getAllTrips()) {
			stats.addTrip(tripData);
		}
		return stats;
	}

	private void connectToService() {
		Intent rService = new Intent(this, RecordingService.class);
		ServiceConnection sc = new ServiceConnection() {

			public void onServiceDisconnected(ComponentName name) {
			}

			public void onServiceConnected(ComponentName name, IBinder service) {
				RecordingService.IRecordService rs = (RecordingService.IRecordService)service;
				int state = rs.getState();
				if (state == RecordingService.STATE_RECORDING || state == RecordingService.STATE_PAUSED) {
					startActivity(new Intent(MainActivity.this, RecordingActivity.class));
				} else {
					cleanupData();
				}
				MainActivity.this.unbindService(this); // race?  this says we no longer care
			}
		};
		// This needs to block until the onServiceConnected (above) completes.
		// Thus, we can check the recording status before continuing on.
		bindService(rService, sc, Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onResume() {
		super.onResume();
		// Check that Google Play services is available
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (ConnectionResult.SUCCESS == resultCode) {
			Log.d("Location Updates", "Google Play services is available.");
			return;
			// Google Play services was not available for some reason
		} else {
			// Get the error dialog from Google Play services
			Dialog errorDialog = GooglePlayServicesUtil
					.getErrorDialog(resultCode, this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
			// If Google Play services can provide an error dialog
			if (errorDialog != null) {
				ErrorDialogFragment errorFragment = new ErrorDialogFragment();
				errorFragment.setDialog(errorDialog);
				errorFragment.show(getSupportFragmentManager(), "Location Updates");
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
			@NonNull int[] grantResults) {
		if (requestCode == FINE_LOCATION_REQUEST_CODE) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				proccessWithLocationPermission();
			} else {
				Toast.makeText(this, "Ohne GPS-Berechtigung kann die App nicht ausgeführt werden.", Toast.LENGTH_SHORT)
						.show();
			}
		} else
			super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	private void proccessWithLocationPermission() {
		final LocationManager manager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			buildAlertMessageNoGps();
		} else {
			startActivity(new Intent(this, RecordingActivity.class));
		}
	}

	private void buildAlertMessageNoGps() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(
				"Your phone's GPS is disabled. Cycle Philly needs GPS to determine your location.\n\nGo to System Settings now to enable GPS?")
				.setCancelable(false)
				.setPositiveButton("GPS Settings...", new DialogInterface.OnClickListener() {

					public void onClick(final DialogInterface dialog, final int id) {
						final Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
						startActivityForResult(intent, 0);
					}
				})
				.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

					public void onClick(final DialogInterface dialog, final int id) {
						dialog.cancel();
					}
				});
		final AlertDialog alert = builder.create();
		alert.show();
	}

	void cleanupData() {
		DbAdapter mDb = new DbAdapter(MainActivity.this);
		int cleanedTrips = mDb.cleanTables();
		if (cleanedTrips > 0) {
			Toast.makeText(getBaseContext(), "" + cleanedTrips + " bad trip(s) removed.", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, CONTEXT_RETRY, 0, "Retry Upload");
		menu.add(0, CONTEXT_DELETE, 0, "Delete");
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		switch (item.getItemId()) {
			case CONTEXT_RETRY:
				retryTripUpload(info.id);
				return true;
			case CONTEXT_DELETE:
				deleteTrip(info.id);
				return true;
			default:
				return super.onContextItemSelected(item);
		}
	}

	private void retryTripUpload(long tripId) {
	}

	private void deleteTrip(long tripId) {
		DbAdapter mDbHelper = new DbAdapter(this);
		mDbHelper.deleteTrip(tripId);
	}



	/* Handles item selections */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case MENU_LEGAL_INFO:
				startActivity(new Intent(this, LicenseActivity.class));
				return true;
		}
		return false;
	}
}
