package de.grundid.plusrad.recording;

import android.location.Location;
import de.grundid.plusrad.db.TripData;

public class CurrentTrip {

	private TripData tripData;
	private float distanceTraveled;
	private float maxSpeed;
	private float currentSpeed;
	private int points;
	private Location location;
	private long standingTimeTimestamp;
	private long pauseTimestamp;

	public CurrentTrip(TripData tripData) {
		this.tripData = tripData;
		this.pauseTimestamp = System.currentTimeMillis();
	}

	public CurrentTrip(TripData tripData, float distanceTraveled, float maxSpeed, float currentSpeed, int points,
			long standingTimeTimestamp, long pauseTimestamp) {
		this.tripData = tripData;
		this.distanceTraveled = distanceTraveled;
		this.maxSpeed = maxSpeed;
		this.currentSpeed = currentSpeed;
		this.points = points;
		this.standingTimeTimestamp = standingTimeTimestamp;
		this.pauseTimestamp = pauseTimestamp;
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
			return standingTimeTimestamp + (System.currentTimeMillis() - pauseTimestamp);
		} else {
			return standingTimeTimestamp;
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
			if (pauseTimestamp != -1) {
				standingTimeTimestamp += System.currentTimeMillis() - pauseTimestamp;
				pauseTimestamp = -1;
			}
		} else {
			if (pauseTimestamp == -1) {
				pauseTimestamp = System.currentTimeMillis();
			}
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

	public long getStandingTimeTimestamp() {
		return standingTimeTimestamp;
	}

	public long getPauseTimestamp() {
		return pauseTimestamp;
	}
}
