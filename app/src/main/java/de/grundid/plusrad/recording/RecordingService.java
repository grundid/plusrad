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
package de.grundid.plusrad.recording;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import de.grundid.plusrad.R;
import de.grundid.plusrad.db.TripData;

@SuppressWarnings("MissingPermission")
public class RecordingService extends Service implements LocationListener {

	private final static int NOTIFICATION_ID = 1;
	public final static int STATE_IDLE = 0;
	public final static int STATE_RECORDING = 1;
	public final static int STATE_PAUSED = 2;
	public static final int MAX_TRACKING_SPEED = 60; // meter / sec
	public static final int MIN_TRACKING_ACCURACY = 20; // meter
	public static final int MIN_TIME_PRO_LOCATION_UPDATE = 1000;
	public static final String TAG = "PRAD";
	private static final String CURRENT_STATE = "currentState";
	private static final String CURRENT_TRIP_ID = "currentTridId";
	private static final String DISTANCE_TRAVELED = "distanceTraveled";
	private static final String MAX_SPEED = "maxSpeed";
	private static final String CURRENT_SPEED = "currentSpeed";
	private static final String POINTS = "points";
	private static final String STANDING_TIME_TIMESTAMP = "standingTimeTimestamp";
	private static final String PAUSE_TIMESTAMP = "pauseTimestamp";
	private static final String[] ALL_FIELDS = { CURRENT_STATE, CURRENT_TRIP_ID, DISTANCE_TRAVELED, MAX_SPEED,
			CURRENT_SPEED, POINTS,
			STANDING_TIME_TIMESTAMP, PAUSE_TIMESTAMP };

	public interface IRecordService {

		int getState();

		CurrentTrip startRecording();

		void cancelRecording();

		CurrentTrip finishRecording();

		CurrentTrip getCurrentTrip();

		void pauseRecording();

		void resumeRecording();

		void reset();

		void setListener(UpdateListener updateListener);
	}

	public interface UpdateListener {

		void updateStatus(CurrentTrip currentTrip);
	}

	private UpdateListener updateListener;
	private LocationManager lm = null;
	private CurrentTrip trip;
	private DbAdapter dbAdapter;
	private int state = STATE_IDLE;

	@Override
	public IBinder onBind(Intent intent) {
		return new RecordingServiceBinder(this);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "recording service create");
		lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		dbAdapter = new DbAdapter(getApplicationContext());
	}

	@Override
	public int onStartCommand(Intent intent,
			int flags,
			int startId) {
		if (intent == null) {
			resumeTracking();
		}
		return super.onStartCommand(intent, flags, startId);
	}

	private void resumeTracking() {
		Log.i(TAG, "restarting tracking...");
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		int testState = sharedPreferences.getInt(CURRENT_STATE, -1);
		long testTripId = sharedPreferences.getLong(CURRENT_TRIP_ID, -1);
		if (testState != -1 && testTripId != -1) {
			this.state = testState;
			TripData tripData = dbAdapter.getTrip(testTripId);
			float distanceTraveled = sharedPreferences.getFloat(DISTANCE_TRAVELED, 0);
			float maxSpeed = sharedPreferences.getFloat(MAX_SPEED, 0);
			float currentSpeed = sharedPreferences.getFloat(CURRENT_SPEED, 0);
			int points = sharedPreferences.getInt(POINTS, 0);
			long standingTimeTimestamp = sharedPreferences.getLong(STANDING_TIME_TIMESTAMP, 0);
			long pauseTimestamp = sharedPreferences.getLong(PAUSE_TIMESTAMP, 0);
			trip = new CurrentTrip(tripData, distanceTraveled, maxSpeed, currentSpeed, points, standingTimeTimestamp,
					pauseTimestamp);
			if (state == STATE_RECORDING) {
				startLocationUpdates();
			}
		}
	}

	@Override
	public void onTrimMemory(int level) {
		saveStateInPref();
	}

	private void saveStateInPref() {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putInt(CURRENT_STATE, getState());
		editor.putLong(CURRENT_TRIP_ID, trip.getTripData().getTripId());
		editor.putFloat(DISTANCE_TRAVELED, trip.getDistanceTraveled());
		editor.putFloat(MAX_SPEED, trip.getMaxSpeed());
		editor.putFloat(CURRENT_SPEED, trip.getCurrentSpeed());
		editor.putInt(POINTS, trip.getPoints());
		editor.putLong(STANDING_TIME_TIMESTAMP, trip.getStandingTimeTimestamp());
		editor.putLong(PAUSE_TIMESTAMP, trip.getPauseTimestamp());
		editor.apply();
	}

	private void cleaStateInPref() {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		for (String fieldName : ALL_FIELDS) {
			editor.remove(fieldName);
		}
		editor.apply();
	}

	public CurrentTrip startRecording() {
		this.state = STATE_RECORDING;
		TripData tripData = dbAdapter.createTrip();
		this.trip = new CurrentTrip(tripData);
		setNotification();
		startLocationUpdates();
		return this.trip;
	}

	private void startLocationUpdates() {
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_PRO_LOCATION_UPDATE, 0, this);
	}

	public void pauseRecording() {
		this.state = STATE_PAUSED;
		lm.removeUpdates(this);
	}

	public void resumeRecording() {
		this.state = STATE_RECORDING;
		startLocationUpdates();
	}

	public CurrentTrip finishRecording() {
		this.state = STATE_IDLE;
		lm.removeUpdates(this);
		TripData tripData = trip.getTripData();
		tripData.setEndTime(System.currentTimeMillis());
		tripData.setTripTime(trip.getRindingTime());
		tripData.setDistance(trip.getDistanceTraveled());
		tripData.setStatus(DbAdapter.STATUS_COMPLETE);
		dbAdapter.updateTrip(tripData);
		clearNotifications();
		cleaStateInPref();
		return trip;
	}

	public void cancelRecording() {
		lm.removeUpdates(this);
		if (trip != null) {
			dbAdapter.deleteTrip(trip.getTripData().getTripId());
		}
		clearNotifications();
		cleaStateInPref();
		this.state = STATE_IDLE;
	}

	@Override
	public void onLocationChanged(Location loc) {
		if (loc != null) {
			//Log.i(TAG, "New Location " + loc);
			updateTripStats(loc);
			savePointInDatabase(loc, System.currentTimeMillis());
			notifyListeners();
			// TODO update pref state every minute
		}
	}

	private void savePointInDatabase(Location loc, long currentTime) {
		double lat = loc.getLatitude();
		double lgt = loc.getLongitude();
		float accuracy = loc.getAccuracy();
		double altitude = loc.getAltitude();
		float speed = loc.getSpeed();
		CyclePoint pt = new CyclePoint(lat, lgt, currentTime, accuracy, altitude, speed);
		trip.incPoints();
		// TODO update endTime and bounding box
		//endTime = currentTime - this.totalPauseTime;

		/*latlow = Math.min(latlow, lat);
		lathigh = Math.max(lathigh, lat);
		lgtlow = Math.min(lgtlow, lgt);
		lgthigh = Math.max(lgthigh, lgt);*/
		dbAdapter.addCoordToTrip(trip.getTripData().getTripId(), pt);
	}

	@Override
	public void onProviderDisabled(String provider) {
		Log.i(TAG, "provider disabled: " + provider);
	}

	@Override
	public void onProviderEnabled(String provider) {
		Log.i(TAG, "provider enabled: " + provider);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		Log.i(TAG, "onStatusChanged: " + provider + " status: " + status);
	}

	private void setNotification() {
		Context context = this;
		String contentTitle = "PlusRad - Aufzeichnung";
		String contentText = "Klicken für mehr Details";
		Intent notificationIntent = new Intent(context, RecordingActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
		Notification notification = new NotificationCompat.Builder(context)
				.setSmallIcon(R.drawable.ic_directions_bike_white_24dp)
				.setContentTitle(contentTitle)
				.setContentText(contentText)
				.setColor(context.getResources().getColor(R.color.colorPrimary))
				.setContentIntent(pendingIntent)
				.setOngoing(true)
				.setAutoCancel(false).build();
		NotificationManager notificationManager =
				(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(NOTIFICATION_ID, notification);
	}

	private void clearNotifications() {
		NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(NOTIFICATION_ID);
	}

	private void updateTripStats(Location newLocation) {
		float curSpeed = newLocation.getSpeed();
		if (curSpeed < MAX_TRACKING_SPEED) {
			trip.updateSpeed(curSpeed);
		}
		trip.updatePause(curSpeed);
		if (newLocation.getAccuracy() <= MIN_TRACKING_ACCURACY) {
			if (trip.getLocation() != null) {
				trip.addDistance(trip.getLocation().distanceTo(newLocation));
			}
			trip.setLocation(newLocation);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (state == STATE_RECORDING) {
			finishRecording();
		}
	}

	void notifyListeners() {
		if (updateListener != null) {
			updateListener.updateStatus(trip);
		}
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	public CurrentTrip getTrip() {
		return trip;
	}

	public void setUpdateListener(UpdateListener updateListener) {
		this.updateListener = updateListener;
		notifyListeners();
	}
}
