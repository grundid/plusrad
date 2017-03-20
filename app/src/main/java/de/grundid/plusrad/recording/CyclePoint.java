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

import com.google.android.gms.maps.model.LatLng;

public class CyclePoint {

	private LatLng coords;
	private float accuracy;
	private double altitude;
	private float speed;
	private long time;
	private int activity;

	public CyclePoint(double lat, double lgt, long currentTime) {
		coords = new LatLng(lat, lgt);
		this.time = currentTime;
	}

	public CyclePoint(double lat, double lgt, long currentTime, float accuracy, double altitude, float speed) {
		coords = new LatLng(lat, lgt);
		this.time = currentTime;
		this.accuracy = accuracy;
		this.altitude = altitude;
		this.speed = speed;
	}

	public LatLng getCoords() {
		return coords;
	}

	public float getAccuracy() {
		return accuracy;
	}

	public double getAltitude() {
		return altitude;
	}

	public float getSpeed() {
		return speed;
	}

	public long getTime() {
		return time;
	}

	public int getActivity() {
		return activity;
	}
}

