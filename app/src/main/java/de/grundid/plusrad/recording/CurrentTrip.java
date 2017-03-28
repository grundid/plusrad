package de.grundid.plusrad.recording;

import android.location.Location;
import de.grundid.plusrad.db.TripData;

public class CurrentTrip {

	public static final int MIN_TRACKING_ACCURACY = 20; // meter

	private TripData tripData;
	private float distanceTraveled;
	private float maxSpeed;
	private float currentSpeed;
	private int points;
	private Location location;
	private long standingTime;
	private long pauseTimestamp;
	private boolean manualPause;

	public CurrentTrip(TripData tripData) {
		this.tripData = tripData;
		this.pauseTimestamp = System.currentTimeMillis();
	}

	public CurrentTrip(TripData tripData, float distanceTraveled, float maxSpeed, float currentSpeed, int points,
			long standingTime, long pauseTimestamp, boolean manualPause) {
		this.tripData = tripData;
		this.distanceTraveled = distanceTraveled;
		this.maxSpeed = maxSpeed;
		this.currentSpeed = currentSpeed;
		this.points = points;
		this.standingTime = standingTime;
		this.pauseTimestamp = pauseTimestamp;
		this.manualPause = manualPause;
	}

	public static boolean isLocationAccurate(Location location) {
		return location != null && location.getAccuracy() < MIN_TRACKING_ACCURACY;
	}

	public boolean hasTrackData() {
		return points > 0;
	}

	public TripData getTripData() {
		return tripData;
	}

	public float getMaxSpeed() {
		return maxSpeed;
	}

	public float getCurrentSpeed() {
		return currentSpeed;
	}

	public void updateSpeed(float speed) {
		currentSpeed = speed;
		maxSpeed = Math.max(maxSpeed, speed);
	}

	public float getDistanceTraveled() {
		return distanceTraveled;
	}

	public void addDistance(float distance) {
		distanceTraveled += distance;
	}

	public void incPoints() {
		points++;
	}

	public long getTripDuration() {
		return System.currentTimeMillis() - tripData.getStartTime();
	}

	public long getRindingTime() {
		return getTripDuration() - getStandingTime();
	}

	public long getStandingTime() {
		if (pauseTimestamp != -1) {
			return standingTime + getPauseDuration();
		} else {
			return standingTime;
		}
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public void updatePause(float currentSpeed) {
		if (currentSpeed > 0.6) {
			resumePause(false);
		} else {
			pause(false);
		}
	}

	public void pause() {
		pause(true);
	}

	public void resumePause() {
		resumePause(true);
	}

	public boolean isManualPause() {
		return manualPause;
	}

	private void resumePause(boolean manual) {
		if (pauseTimestamp != -1) {
			standingTime += getPauseDuration();
			pauseTimestamp = -1;
		}
		if (manual) {
			manualPause = false;
		}
	}

	private long getPauseDuration() {
		return System.currentTimeMillis() - pauseTimestamp;
	}

	private void pause(boolean manual) {
		if (pauseTimestamp == -1) {
			pauseTimestamp = System.currentTimeMillis();
		}
		if (manual) {
			manualPause = true;
		}
	}

	public boolean isPause() {
		return pauseTimestamp != -1;
	}

	public double getAvgRidingTime() {
		if (getRindingTime() > 1000) {
			return getDistanceTraveled() / (getRindingTime() / 1000.0);
		} else {
			return Double.NaN;
		}
	}

	public double getAvgTotalTime() {
		if (getTripDuration() > 1000) {
			return getDistanceTraveled() / (getTripDuration() / 1000.0);
		} else {
			return Double.NaN;
		}
	}

	public int getPoints() {
		return points;
	}

	public long getPauseTimestamp() {
		return pauseTimestamp;
	}

	public void finishRecording() {
		if (isPause()) {
			standingTime += getPauseDuration();
		}
	}
}
